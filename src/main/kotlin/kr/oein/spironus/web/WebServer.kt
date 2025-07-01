package kr.oein.spironus.web

import io.javalin.Javalin;
import io.javalin.http.servlet.JavalinServletContext
import kr.oein.spironus.Spironus
import java.util.Timer
import kotlin.collections.mapOf

class WebServer(val spironus: Spironus) {
    private val app: Javalin = Javalin.create { config ->
        config.showJavalinBanner = false
    }

    var adminPassword = ""
    val adminTokens = mutableSetOf<String>()

    init {
        app.get("/") { ctx ->
            ctx.redirect("/index.html")
        }

        app.before("/adminapi/*") { ctx ->
            spironus.logger.info { "Admin API accessed: ${ctx.path()}" }
            val token = ctx.queryParam("token")
            if (token == null || !validateToken(token)) {
                ctx.status(403).result("Forbidden: Invalid or missing token")
                (ctx as JavalinServletContext).tasks.clear()
            }
        }

        app.get("/adminapi/status") { ctx ->
            ctx.result("Server is running")
        }

        setPasswordFromConfig()
        initializePasswordValidator()

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
            if (token != null && validateToken(token)) {
                ctx.result("Token is valid")
            } else {
                ctx.status(403).result("Forbidden: Invalid token")
            }
        }
    }

    fun generateToken(): String {
        return java.util.UUID.randomUUID().toString()
    }

    fun validateToken(token: String): Boolean {
        return adminTokens.contains(token)
    }

    fun start(port: Int) {
        app.start(port)
    }

    fun stop() {
        app.stop()
    }
}