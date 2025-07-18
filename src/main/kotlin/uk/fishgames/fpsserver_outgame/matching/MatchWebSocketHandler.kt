package uk.fishgames.fpsserver_outgame.matching

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import uk.fishgames.fpsserver_outgame.matching.dto.EnsureMatchDto
import uk.fishgames.fpsserver_outgame.matching.dto.MatchWsEventType
import uk.fishgames.fpsserver_outgame.matching.dto.WsEventDto
import uk.fishgames.fpsserver_outgame.matching.dto.MatchWsEventType.*
import uk.fishgames.fpsserver_outgame.matching.dto.SessionAttributesEnum
import uk.fishgames.fpsserver_outgame.matching.dto.TryCharacterPickDto
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

//매칭 시에 연결되는 웹소켓 핸들러
@Component
class MatchWebSocketHandler (
    private val matchService: MatchService,
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
        matchService.cancelPlayer(session)
    }
    var pickLock: Lock = ReentrantLock()
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


                        val requestDto = try{Json.decodeFromJsonElement<String>(dto.Message)}//현재는 선택한 게임 모드 - 추후 더 전달할거있으면 여기
                        catch (e:Exception){session.close(CloseStatus.BAD_DATA);return;}

                        logger.info { "try enqueue match user:${session.attributes[SessionAttributesEnum.userId.value]} | | | Mode:${requestDto}" }

                        val userId:String = session.attributes[SessionAttributesEnum.userId.value] as String;
                        val playerDto = matchService.createPlayerDtoFromDataBase(userId, session)

                        if(playerDto == null) {session.close(CloseStatus.BAD_DATA);return;}

                        session.attributes.set(SessionAttributesEnum.userKey.value,playerDto.key)

                        val gameMode = GameMode.valueOf(requestDto)

                        matchService.cancelPlayer(session);
                        val success = matchService.registerPlayer(session,playerDto, gameMode)
                        if(!success) {session.close(CloseStatus.SERVER_ERROR);return;}

                        session.sendMessage(TextMessage(WsEventDto.ensureEnqueue(EnsureMatchDto(playerDto.key))))

                        matchService.tryMakeMatch(gameMode)
                    }
                    catch (e: Exception) {
                        logger.error { ("Exception while connecting to client: ${e.message}") } ;
                        session.close(CloseStatus.SERVER_ERROR)
                    }
                }

                MatchWsEventType.PickCharacter -> {
                    if(dto.Message == null) {session.close(CloseStatus.BAD_DATA);return}

                    val requestDto = try {
                        Json.decodeFromJsonElement<TryCharacterPickDto>(dto.Message)
                    } catch (e: Exception) {
                        session.close(CloseStatus.BAD_DATA)
                        return
                    }

                    val gameSession = GameSessionHolder.runningSessions[requestDto.sessionId]
                    if (gameSession == null) {
                        session.close(CloseStatus.BAD_DATA)
                        return
                    }

                    val notifyDto = gameSession.pickCharacterUp(requestDto)

                    //실패 시
                    if(notifyDto == null) return;

                    //성공 시 - TryCharacterPickDto 그대로 반환 전송
                    else {
                        gameSession.broadcastToAllPlayer(notifyDto)
                        session.sendMessage(
                            TextMessage(
                                Json.encodeToString(
                                    WsEventDto(
                                        MatchWsEventType.PickCharacterSuccess,
                                        dto.Message)
                                )
                            )
                        )
                    }
                }
                MatchWsEventType.PickCharacterTemporary ->{
                    if(dto.Message == null) {session.close(CloseStatus.BAD_DATA);return}
                    val requestDto = try {
                        Json.decodeFromJsonElement<TryCharacterPickDto>(dto.Message)
                    } catch (e: Exception) {
                        println("Exception while connecting to client: ${e.message}") ;
                        session.close(CloseStatus.BAD_DATA)
                        return
                    }
                    val gameSession = GameSessionHolder.runningSessions[requestDto.sessionId]
                    if (gameSession == null) {
                        println("Invalid Game Session!")
                        session.close(CloseStatus.BAD_DATA)
                        return
                    }
                    val notifyDto = gameSession.pickCharacterOn(requestDto)
                    if(notifyDto == null){
                        println("notifyDto is null"); return
                    }
                    else {
                        gameSession.broadcastToAllPlayer(notifyDto)
                    }
                    println("Broadcast PickCharacterOn Message To Session")
                }
                MatchWsEventType.GetPickInformation -> {
                    TODO()
                }
                MatchWsEventType.Pong -> {
                    TODO()
                }

                else-> {
                    return
                }
            }
        }catch (e:Exception){
            session.close(CloseStatus.BAD_DATA)
            println(message.payload)
            return;
        }

    }
}
