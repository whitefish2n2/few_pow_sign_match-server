package uk.fishgames.fpsserver_outgame.matching

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import uk.fishgames.fpsserver_outgame.matching.dto.WsEventDto
import uk.fishgames.fpsserver_outgame.matching.dto.WsEventType.*

//인터럽터에서 매칭 큐 등록은 완료됨.
//이후에 메시지 처리할 일 있으면 사용하도록 만들어둔 핸들러임을 명시한다.
@Component
class MatchWebSocketHandler (
    private val registry: MatchRegistry,
    private val queueManager: MatchQueueManager,
    private val matchService: MatchService
) : TextWebSocketHandler() {
    val logger = KotlinLogging.logger {}
    override fun afterConnectionEstablished(session: WebSocketSession) {
        try {
            //try to enqueue player
            logger.info { "Websocket Connected: ${session.attributes["userId"]}" }
            matchService.registerPlayer(session)
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
        try {
            println("Received from client: ${message.payload}")
            val dto = Json.decodeFromString<WsEventDto>(message.payload)
            when(dto.typeEnum()){
                Cancel -> {
                    session.close()
                    return
                }

                MatchFound -> {
                    println("니가 이걸 왜보내냐")
                    session.close()
                    return
                }
            }
        }catch (e:Exception){
            println(message.payload)
            return;
        }

    }

    private fun extractPlayerId(session: WebSocketSession): String {
        val query = session.uri?.query ?: throw IllegalArgumentException("Missing query")
        return session.attributes["playerId"].toString()
    }
}
