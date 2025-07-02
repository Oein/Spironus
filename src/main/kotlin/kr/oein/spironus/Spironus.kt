package kr.oein.spironus

import kr.oein.interchest.GUIListener
import kr.oein.interchest.GUIManager
import kr.oein.spironus.components.Chat
import kr.oein.spironus.components.KVDB
import kr.oein.spironus.components.Whitelist
import kr.oein.spironus.web.WebServer
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin


class Spironus : JavaPlugin() {
    var kvdb = KVDB(this)

    val guiManager = GUIManager()
    val guiListener = GUIListener(guiManager)
    val webServer = WebServer(this)
    override fun onEnable() {

        Bukkit.getPluginManager().registerEvents(guiListener, this)
        Bukkit.getPluginManager().registerEvents(Whitelist(this), this)
        Bukkit.getPluginManager().registerEvents(Chat(this), this)

        saveDefaultConfig()
        config.get("listen") ?.let {
            if (it is Number && it.toInt() > 0) {
                logger.info("Listening on port: ${it.toInt()}")
                webServer.start(it.toInt())
            } else logger.warning("Invalid 'listen' configuration, expected a number but got: $it")
        } ?: run {
            logger.warning("No 'listen(int, 1 - 65535)' configuration found, please check your config.yml")
        }
    }

    override fun onDisable() {
        webServer.stop()
    }
}
