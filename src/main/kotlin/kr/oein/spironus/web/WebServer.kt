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

    val teamMasterTokens = mutableMapOf<String, String>() // Token 2 Team ID
    val webAdmin = WebAdmin(spironus, app)

    init {
        app.get("/") { ctx ->
            ctx.redirect("/index.html")
        }

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

    private fun generateToken(): String {
        return java.util.UUID.randomUUID().toString()
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

    /**
     * Validates a team master token and returns the associated team ID if valid.
     * @param token The team master token to validate.
     * @return The team ID if the token is valid, null otherwise.
     */
    public fun validateTeamMasterToken(token: String): String? {
        return teamMasterTokens[token]
    }

    public fun start(port: Int) {
        app.start(port)
    }

    public fun stop() {
        app.stop()
    }
}