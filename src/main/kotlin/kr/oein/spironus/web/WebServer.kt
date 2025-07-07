package kr.oein.spironus.web

import io.javalin.Javalin
import kr.oein.spironus.Spironus

class WebServer(val spironus: Spironus) {
    private val app: Javalin = Javalin.create { config ->
        config.showJavalinBanner = false
    }

    val webAdmin = WebAdmin(spironus, app)
    val webTeam = WebTeam(spironus, app)

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

    fun start(port: Int) {
        app.start(port)
    }

    fun stop() {
        app.stop()
    }
}