package uk.fishgames.fpsserver_outgame.matching.ws

import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Component
class MatchWebsocketRegistry {
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    fun register(playerKey: String, session: WebSocketSession) {
        sessions[playerKey] = session
    }

    fun remove(playerKey: String) {

        val o = sessions.remove(playerKey)
    }

    fun get(playerKey: String): WebSocketSession? = sessions[playerKey]
}