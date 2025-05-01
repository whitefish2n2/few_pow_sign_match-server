package uk.fishgames.fpsserver_outgame.matching

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import uk.fishgames.fpsserver_outgame.PlayerNotFoundException
import uk.fishgames.fpsserver_outgame.matching.dto.WsEventDto
import uk.fishgames.fpsserver_outgame.matching.dto.WsEventType

//인터럽터에서 매칭 큐 등록은 완료됨.
//이후에 메시지 처리할 일 있으면 사용하도록 만들어둔 핸들러임을 명시한다.
@Component
class MatchWebSocketHandler (
    private val registry: MatchRegistry,
    private val queueManager: MatchQueueManager,
    private val matchService: MatchService
) : TextWebSocketHandler() {
    override fun afterConnectionEstablished(session: WebSocketSession) {
        try {
            //try to enqueue player
            val userId:String = session.attributes["userId"].toString()
            val modeOrdinal: Int? = (session.attributes["gameMode"] as? String)?.toIntOrNull()
            val mode = GameMode.fromId(modeOrdinal)
            if(userId == "" || mode == null) return
            val dto = matchService.getNewPlayerDto(userId)
            queueManager.enqueue(mode,userId,dto)
            val newSession = matchService.tryMakeMatch(mode)?:return
            sendMatchComplete(userId,newSession.runningOn.ip,8888,newSession.gameId)
        }
        catch (e: Exception) {
            println("Exception while attempting to send match")
            println(e.message)
            session.close()
        }

    }
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val playerId = extractPlayerId(session)
        registry.remove(playerId)
        queueManager.cancel(playerId)
        println("WebSocket disconnected: $playerId")
    }
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        // ping/pong 또는 클라이언트 확인 메시지 대응 시 사용
        println("Received from client: ${message.payload}")
        val dto = Json.decodeFromString<WsEventDto>(message.payload)
        when(dto.typeEnum()){
            WsEventType.Cancel -> {
                session.close()
            }
        }
        val responseMessage = "whats wrong with you; hacker"
        session.sendMessage(TextMessage(responseMessage))
    }

    private fun extractPlayerId(session: WebSocketSession): String {
        val query = session.uri?.query ?: throw IllegalArgumentException("Missing query")
        return session.attributes["playerId"].toString()
    }

    fun sendMatchFound(session: WebSocketSession){

    }
    fun sendMatchComplete(playerId: String, ip: String, port: Int, gameId:String) {
        val session = registry.get(playerId)
        if (session?.isOpen == true) {
            val payload = mapOf(
                "type" to "match_complete",
                "serverIp" to ip,
                "port" to port
            )
            val message = ObjectMapper().writeValueAsString(payload)
            session.sendMessage(TextMessage(message))
        }
    }
}
