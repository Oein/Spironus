package kr.oein.spironus.web

import io.javalin.Javalin
import io.javalin.http.servlet.JavalinServletContext
import java.util.Timer
import kr.oein.spironus.Spironus
import kr.oein.spironus.components.Random

class WebTeam(val spironus: Spironus, val app: Javalin) {
    val kvdb = spironus.kvdb

    val teamMasterTokens = mutableMapOf<String, String>() // Token 2 Team ID

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
}