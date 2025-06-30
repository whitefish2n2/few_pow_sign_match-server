package uk.fishgames.fpsserver_outgame.matching

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import uk.fishgames.fpsserver_outgame.matching.dto.MatchWsEventType
import uk.fishgames.fpsserver_outgame.matching.dto.WsEventDto
import uk.fishgames.fpsserver_outgame.matching.dto.MatchWsEventType.*
import uk.fishgames.fpsserver_outgame.matching.dto.SessionAttributesEnum

//매칭 시에 연결되는 웹소켓 핸들러
@Component
class MatchWebSocketHandler (
    private val matchService: MatchService
) : TextWebSocketHandler() {
    val logger = KotlinLogging.logger {}
    override fun afterConnectionEstablished(session: WebSocketSession) {
        try {
            logger.info { "Websocket Connected: ${session.attributes[SessionAttributesEnum.userId.value]}" }
        }
        catch (e: Exception) {
            logger.error { ("Exception while Connect to WebSocket: ${e.message}") } ;
            session.close(CloseStatus.SERVER_ERROR)
        }

    }
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        matchService.cancelPlayer(session, status)
    }
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            println("Received from client: ${message.payload}")
            val dto = Json.decodeFromString<WsEventDto>(message.payload)
            when(dto.Type){
                Ping->{
                    logger.info { "Ping From ${session.attributes[SessionAttributesEnum.userId.value]}" }
                    session.sendMessage(TextMessage(WsEventDto.pong,true))
                    logger.info { "send Pong to ${session.attributes[SessionAttributesEnum.userId.value]}" }
                }
                EnqueueMatch -> {
                    try {

                        if(dto.Message == null) {session.close(CloseStatus.BAD_DATA);return}

                        val requestDto = Json.decodeFromJsonElement<String>(dto.Message);//현재는 선택한 게임 모드 - 추후 더 전달할거있으면 여기

                        logger.info { "try enqueue match user:${session.attributes[SessionAttributesEnum.userId.value]} | | | Mode:${requestDto}" }

                        val userId:String = session.attributes[SessionAttributesEnum.userId.value] as String;
                        val playerDto = matchService.CreatePlayerDtoFromDataBase(userId, session)

                        if(playerDto == null) {session.close(CloseStatus.BAD_DATA);return;}

                        session.attributes.set(SessionAttributesEnum.userKey.value,playerDto.key)


                        val success = matchService.registerPlayer(session,playerDto, GameMode.valueOf(requestDto))
                        if(!success) {session.close(CloseStatus.SERVER_ERROR);return;}
                    }
                    catch (e: Exception) {
                        logger.error { ("Exception while connecting to client: ${e.message}") } ;
                        session.close(CloseStatus.SERVER_ERROR)
                    }
                }

                MatchFound -> {
                    println("니가 이걸 왜보내냐")
                    session.close()
                    return
                }

                EnsureEnqueueMatch -> TODO()
                MatchWsEventType.PickCharacter -> TODO()
                MatchWsEventType.GetPickInformation -> TODO()
                MatchWsEventType.Pong -> TODO()

                else->return
            }
        }catch (e:Exception){
            println(message.payload)
            return;
        }

    }
}
