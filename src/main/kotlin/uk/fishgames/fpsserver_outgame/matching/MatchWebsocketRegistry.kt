package uk.fishgames.fpsserver_outgame.matching

import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Component
class MatchWebsocketRegistry {
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    fun register(playerId: String, session: WebSocketSession) {
        sessions[playerId] = session
    }

    fun remove(playerId: String) {
        sessions.remove(playerId)
    }

    fun get(playerId: String): WebSocketSession? = sessions[playerId]
}

