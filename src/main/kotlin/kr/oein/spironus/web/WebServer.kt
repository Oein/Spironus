package kr.oein.spironus.web

import io.javalin.Javalin;
import kr.oein.spironus.Spironus
import java.util.Timer
import kotlin.collections.mapOf

class WebServer(spironus: Spironus) {
    private val app: Javalin = Javalin.create { config ->
        config.showJavalinBanner = false
    }

    var adminPassword = "1234"
    val adminTokens = mutableSetOf<String>()

    init {
        val adminPW = spironus.config.get("password")
        if (adminPW is String && adminPW.isNotEmpty()) {
            adminPassword = adminPW
        }

        app.get("/") { ctx ->
            ctx.redirect("/index.html")
        }

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
            } else {
                ctx.status(403).result("Forbidden: Invalid password")
            }
        }

        app.get("/*") { ctx ->
            ctx.res().characterEncoding = "UTF-8"

            // Serve static files from the "static" directory
            val staticFile = spironus.getResource("web${ctx.path()}")
            val fileExtension = ctx.path().substringAfterLast('.', "")
            val contentType = when (fileExtension) {
                "html" -> "text/html"
                "css" -> "text/css"
                "js" -> "application/javascript"
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "gif" -> "image/gif"
                else -> "application/octet-stream"
            } + ", charset=UTF-8"
            if (staticFile != null) {
                ctx.contentType(contentType)
                ctx.result(staticFile.readAllBytes())
            } else {
                ctx.status(404).result("File not found")
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