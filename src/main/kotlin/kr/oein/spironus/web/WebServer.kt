package kr.oein.spironus.web

import io.javalin.Javalin
import io.javalin.http.servlet.JavalinServletContext
import kr.oein.spironus.Spironus
import kr.oein.spironus.components.Random
import java.util.Timer
import kotlin.collections.forEach

class WebServer(val spironus: Spironus) {
    private val app: Javalin = Javalin.create { config ->
        config.showJavalinBanner = false
    }

    val kvdb = spironus.kvdb

    var adminPassword = ""
    val adminTokens = mutableSetOf<String>()

    val teamMasterTokens = mutableMapOf<String, String>() // Token 2 Team ID

    init {
        app.get("/") { ctx ->
            ctx.redirect("/index.html")
        }

        app.before("/adminapi/*") { ctx ->
            val token = ctx.queryParam("token")
            if (token == null || !validateAdminToken(token)) {
                ctx.status(403).result("Forbidden: Invalid or missing token")
                (ctx as JavalinServletContext).tasks.clear()
                spironus.logger.info { "Token invalid or missing token" }
            }
        }

        setPasswordFromConfig()
        initializePasswordValidator()
        initializeAdminAPI()

        initializeStaticFiles()
    }

    private fun initializeStaticFiles() {
        app.get("/*") { ctx ->
            ctx.res().characterEncoding = "UTF-8"

            val staticFile = spironus.getResource("web${ctx.path()}")
            val fileExtension = ctx.path().substringAfterLast('.', "")
            val contentType = when (fileExtension) {
                "html" -> "text/html"
                "css" -> "text/css"
                "js" -> "application/javascript"
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "webp" -> "image/webp"
                else -> "application/octet-stream"
            } + ", charset=UTF-8"
            if (staticFile != null) {
                ctx.contentType(contentType)
                ctx.result(staticFile.readAllBytes())
            } else ctx.status(404).result("File not found")
        }
    }

    private fun setPasswordFromConfig() {
        val adminPW = spironus.config.get("password")
        if (adminPW is String && adminPW.isNotEmpty())
            adminPassword = adminPW
    }

    private fun initializePasswordValidator() {
        app.get("/authorize") { ctx ->
            val password = ctx.queryParam("password")
            if (password == adminPassword) {
                val token = generateToken()
                adminTokens.add(token)
                ctx.result("{\"token\":\"$token\"}")

                val timer = Timer()
                timer.schedule(object : java.util.TimerTask() {
                    override fun run() {
                        adminTokens.remove(token)
                        timer.cancel()
                        timer.purge()
                    }
                }, 60 * 60 * 1000) // Token expires after 1 hour
            } else ctx.status(403).result("Forbidden: Invalid password")
        }

        app.get("/validate") { ctx ->
            val token = ctx.queryParam("token")
            if (token != null && validateAdminToken(token)) {
                ctx.result("Token is valid")
            } else {
                ctx.status(403).result("Forbidden: Invalid token")
            }
        }
    }

    private fun initializeAdminAPI() {
        app.get("/adminapi/status") { ctx ->
            ctx.result("Server is running")
        }

        app.post("/adminapi/reload") { ctx ->
            ctx.result("Server reload requested")
            spironus.server.reload()
        }

        initializeAdminAPI_whitelist()
        initializeAdminAPI_teams()
        initializeAdminAPI_playerinfo()
    }

    private fun initializeAdminAPI_whitelist() {
        app.get("/adminapi/whitelist") { ctx ->
            val whitelist = kvdb.keys("whitelist")
            ctx.json(whitelist)
        }

        app.post("/adminapi/whitelist/add") { ctx ->
            val pid = ctx.queryParam("pid")
            if (pid != null && pid.isNotEmpty()) {
                spironus.logger.info("Adding player with PID: $pid to whitelist")
                ctx.result("Player with PID $pid added to whitelist")
                kvdb.set("whitelist", pid, true)
            } else {
                ctx.status(400).result("Bad Request: Missing or invalid PID")
            }
        }

        app.post("/adminapi/whitelist/remove") { ctx ->
            val pid = ctx.queryParam("pid")
            if (pid != null && pid.isNotEmpty()) {
                spironus.logger.info("Removing player with PID: $pid from whitelist")
                ctx.result("Player with PID $pid removed from whitelist")
                kvdb.remove("whitelist", pid)
            } else {
                ctx.status(400).result("Bad Request: Missing or invalid PID")
            }
        }
    }

    private fun initializeAdminAPI_teams() {
        app.get("/adminapi/kv/teams") { ctx ->
            val teams = kvdb.loadScope("teams").yamlcfg
            val kkv = teams.getKeys(false).associateWith { key ->
                // get teams[key] as Map<*, *>
                val teamData = teams.getConfigurationSection(key)
                if (teamData == null) {
                    ctx.status(404).result("Team not found with ID: $key")
                    return@associateWith mapOf<String, String>()
                }
                mapOf(
                    "name" to teamData.get("name"),
                    "masterPid" to teamData.get("masterPid"),
                )
            }

            ctx.json(kkv)
        }

        app.post("/adminapi/teams/create") { ctx ->
            val teamName = ctx.queryParam("name")
            val masterPID = ctx.queryParam("masterPid")
            if (teamName != null && teamName.isNotEmpty() && masterPID != null && masterPID.isNotEmpty()) {
                var uuid = Random().generate()
                val teamsScope = kvdb.loadScope("teams")

                // check for PID existence
                if (!kvdb.has("whitelist", masterPID)) {
                    ctx.status(400).result("Bad Request: Master PID $masterPID does not exist in whitelist")
                    return@post
                }

                while (teamsScope.has(uuid))
                    uuid = Random().generate()

                val teamConfig = teamsScope.yamlcfg.createSection(uuid)
                teamConfig.set("name", teamName)
                teamConfig.set("masterPid", masterPID)
                teamsScope.save()
            } else {
                ctx.status(400).result("Bad Request: Missing or invalid team name")
            }
        }

        app.post("/adminapi/teams/delete") { ctx ->
            val teamId = ctx.queryParam("teamId")
            if (teamId != null && teamId.isNotEmpty()) {
                val teamsScope = kvdb.loadScope("teams")
                if (teamsScope.has(teamId)) {
                    teamsScope.remove(teamId)
                    // set all players in this team to -1
                    val playersInTeam = kvdb.keys("playerinfo-team").filter { kvdb.get("playerinfo-team", it) == teamId }
                    playersInTeam.forEach { pid ->
                        kvdb.set("playerinfo-team", pid, "-1")
                    }
                    ctx.result("Team with ID $teamId deleted")
                } else {
                    ctx.status(404).result("Team not found with ID: $teamId")
                }
            } else {
                ctx.status(400).result("Bad Request: Missing or invalid team ID")
            }
        }

        app.post("/adminapi/teams/update") { ctx ->
            val teamId = ctx.queryParam("teamId")
            val name = ctx.queryParam("name")
            val masterPid = ctx.queryParam("masterPid")
            if (teamId != null && teamId.isNotEmpty() && (name != null || masterPid != null)) {
                val teamsScope = kvdb.loadScope("teams")
                val child = teamsScope.yamlcfg.getConfigurationSection(teamId)
                if (child == null) {
                    ctx.status(404).result("Team not found with ID: $teamId")
                    return@post
                }
                if (name != null && name.isNotEmpty()) {
                    child.set("name", name)
                }
                if (masterPid != null && masterPid.isNotEmpty()) {
                    // check for PID existence
                    if (!kvdb.has("whitelist", masterPid)) {
                        ctx.status(400).result("Bad Request: Master PID $masterPid does not exist in whitelist")
                        return@post
                    }
                    child.set("masterPid", masterPid)
                }
                teamsScope.save()
                ctx.result("Team with ID $teamId updated")
            } else {
                ctx.status(400).result("Bad Request: Missing or invalid team ID")
            }
        }
    }

    private fun initializeAdminAPI_playerinfo() {
        app.get("/adminapi/kv/playerinfo-nickname") { ctx ->
            val pid = ctx.queryParam("pid")
            if (pid != null && pid.isNotEmpty()) {
                val nickname = kvdb.get("playerinfo-nickname", pid)
                if (nickname != null) {
                    ctx.result(nickname.toString())
                } else {
                    ctx.status(404).result("Nickname not found for PID: $pid")
                }
            } else {
                ctx.status(400).result("Bad Request: Missing or invalid PID")
            }
        }

        app.get("/adminapi/kv/playerinfo-team") { ctx ->
            val pid = ctx.queryParam("pid")
            if (pid != null && pid.isNotEmpty()) {
                val value = kvdb.get("playerinfo-team", pid)
                if (value != null) {
                    ctx.result(value.toString())
                } else {
                    ctx.status(404).result("Team not found for PID: $pid")
                }
            } else {
                ctx.status(400).result("Bad Request: Missing or invalid PID")
            }
        }

        app.post("/adminapi/kv/playerinfo-team/set") { ctx ->
            val pid = ctx.queryParam("pid")
            val team = ctx.queryParam("team")
            if (pid != null && pid.isNotEmpty() && team != null && team.isNotEmpty()) {
                kvdb.set("playerinfo-team", pid, team)
                ctx.result("Team for player with PID $pid set to $team")
            } else {
                ctx.status(400).result("Bad Request: Missing or invalid PID or team")
            }
        }
    }

    private fun generateToken(): String {
        return java.util.UUID.randomUUID().toString()
    }

    public fun validateAdminToken(token: String): Boolean {
        return adminTokens.contains(token)
    }

    public fun issueTeamMasterToken(teamId: String): String {
        val token = generateToken()
        teamMasterTokens[token] = teamId
        val timer = Timer()
        timer.schedule(object : java.util.TimerTask() {
            override fun run() {
                teamMasterTokens.remove(token)
                timer.cancel()
                timer.purge()
            }
        }, 60 * 60 * 1000) // Token expires after 1 hour
        return token
    }

    public fun start(port: Int) {
        app.start(port)
    }

    public fun stop() {
        app.stop()
    }
}