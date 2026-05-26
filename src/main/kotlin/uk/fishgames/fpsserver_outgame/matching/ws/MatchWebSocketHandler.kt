package uk.fishgames.fpsserver_outgame.matching.ws

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import uk.fishgames.fpsserver_outgame.dedicate_server.Session
import uk.fishgames.fpsserver_outgame.matching.GameMode
import uk.fishgames.fpsserver_outgame.matching.GameSessionHolder
import uk.fishgames.fpsserver_outgame.matching.MatchService
import uk.fishgames.fpsserver_outgame.matching.dto.EnsureMatchDto
import uk.fishgames.fpsserver_outgame.matching.dto.MatchWsEventType
import uk.fishgames.fpsserver_outgame.matching.dto.Player
import uk.fishgames.fpsserver_outgame.matching.dto.SessionAttributesEnum
import uk.fishgames.fpsserver_outgame.matching.dto.hoverCharacterDto
import uk.fishgames.fpsserver_outgame.matching.dto.WsEventDto
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

//매칭 시에 연결되는 웹소켓 핸들러
@Component
class MatchWebSocketHandler (
    private val matchService: MatchService,
    val matchWebsocketRegistry: MatchWebsocketRegistry,
    val gameSessionHolder: GameSessionHolder
) : TextWebSocketHandler() {
    val logger = KotlinLogging.logger {}
    override fun afterConnectionEstablished(session: WebSocketSession) {
        try {
            val userId = session.attributes[SessionAttributesEnum.userId.value] as String
            logger.info { "Websocket Connected: $userId" }
            val playerDto = matchService.createPlayerDtoFromDataBase(userId, session)
            session.attributes.set(SessionAttributesEnum.playerInstance.value,playerDto)
            if(playerDto == null) {session.close(CloseStatus.BAD_DATA);return;}
            session.attributes.set(SessionAttributesEnum.userKey.value,playerDto.key)
            matchWebsocketRegistry.register(playerDto.key, session)
        }
        catch (e: Exception) {
            logger.error { ("Exception while Connect to WebSocket: ${e.message}") } ;
            session.close(CloseStatus.SERVER_ERROR)
        }

    }
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        logger.info {  "Player Id : ${session.attributes.get(SessionAttributesEnum.userId.value)} 's Websocket Disconnected: $status" }
        val key = session.attributes.get(SessionAttributesEnum.userKey.value) as String?
        matchWebsocketRegistry.remove(key?:"")
        matchService.cancelPlayer(session)
    }
    var pickLock: Lock = ReentrantLock()
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            println("Received from client: ${message.payload}")
            val dto = Json.Default.decodeFromString<WsEventDto>(message.payload)

            when(dto.Type){
                MatchWsEventType.Ping ->handlePing(session,dto)
                MatchWsEventType.JoinLobby->handleJoinLobby(session,dto)
                MatchWsEventType.EnqueueMatch -> handleEnqueueMatch(session,dto)
                MatchWsEventType.Cancel-> handleCancelMatch(session,dto)
                MatchWsEventType.PickCharacter -> handlePickCharacter(session,dto)
                MatchWsEventType.PickCharacterTemporary -> hoverCharacter(session,dto)
                MatchWsEventType.GetGameTeamPlayerInformation -> handleGetGameTeamPlayerInfo(session,dto)
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
    fun handlePing(session: WebSocketSession, dto: WsEventDto){
        logger.info { "Ping From ${session.attributes[SessionAttributesEnum.userId.value]}" }
        session.sendMessage(TextMessage(WsEventDto.Companion.pong, true))
        logger.info { "send Pong to ${session.attributes[SessionAttributesEnum.userId.value]}" }
    }
    fun handleJoinLobby(session: WebSocketSession, dto:WsEventDto){
        ///todo: 현재 연결되어있는 웹소켓 세션 관리할 땐 여기
    }
    fun handleEnqueueMatch(session: WebSocketSession, dto: WsEventDto){
        try {

            if(dto.Message == null) {session.close(CloseStatus.BAD_DATA);return}


            val requestDto = try{
                Json.Default.decodeFromJsonElement<String>(dto.Message)}//현재는 선택한 게임 모드 - 추후 더 전달할거있으면 여기
            catch (e:Exception){session.close(CloseStatus.BAD_DATA);return;}

            logger.info { "try enqueue match user:${session.attributes[SessionAttributesEnum.userId.value]} | | | Mode:${requestDto}" }

            val userId:String = session.attributes[SessionAttributesEnum.userId.value] as String;

            val playerDto: Player? = session.attributes.get(SessionAttributesEnum.playerInstance.value) as Player?;
            if(playerDto == null) {session.close(CloseStatus.BAD_DATA);return;}

            val gameMode = GameMode.valueOf(requestDto)

            matchService.cancelPlayer(session);
            val success = matchService.registerPlayer(session,playerDto, gameMode)
            if(!success) {session.close(CloseStatus.SERVER_ERROR);return;}

            session.sendMessage(TextMessage(WsEventDto.Companion.ensureEnqueue(EnsureMatchDto(playerDto.key))))

            matchService.tryMakeMatch(gameMode)
        }
        catch (e: Exception) {
            logger.error { ("Exception while connecting to client: ${e.message}") } ;
            session.close(CloseStatus.SERVER_ERROR)
        }
    }
    fun handleCancelMatch(session: WebSocketSession, dto: WsEventDto){
        if(dto.Message == null) {session.close(CloseStatus.BAD_DATA);return}
        matchService.cancelPlayer(session)

        val responseDto = WsEventDto(MatchWsEventType.CancelSuccess, Json.encodeToJsonElement("Cancel Success!"))
        val jsonString = Json.encodeToString(responseDto)
        session.sendMessage(TextMessage(jsonString))
    }
    fun handlePickCharacter(session: WebSocketSession, dto: WsEventDto){
        if(dto.Message == null) {session.close(CloseStatus.BAD_DATA);return}

        val requestDto = try {
            Json.Default.decodeFromJsonElement<hoverCharacterDto>(dto.Message)
        } catch (e: Exception) {
            session.close(CloseStatus.BAD_DATA)
            logger.error { ("Exception while connecting to client: ${e.message}") } ;
            return
        }

        val gameSession = gameSessionHolder.runningSessions[session.attributes.get(SessionAttributesEnum.sessionId.value)]
        if (gameSession == null) {
            session.close(CloseStatus.BAD_DATA)
            return
        }

        val notifyDto = gameSession.pickLockIn(session.attributes[SessionAttributesEnum.userKey.value] as String, requestDto.characterId)

        //실패 시
        if(notifyDto == null) return

        //성공 시 PickCharacterSuccess
        else {
            gameSession.broadcastToAllPlayer(notifyDto)
            session.sendMessage(
                TextMessage(
                    Json.Default.encodeToString(
                        WsEventDto(
                            MatchWsEventType.PickCharacterSuccess,
                            dto.Message
                        )
                    )
                )
            )
        }
    }
    fun hoverCharacter(session: WebSocketSession, dto: WsEventDto){
        if(dto.Message == null) {session.close(CloseStatus.BAD_DATA);return}
        val requestDto = try {
            Json.Default.decodeFromJsonElement<hoverCharacterDto>(dto.Message)
        } catch (e: Exception) {
            println("Exception while connecting to client: ${e.message}") ;
            session.close(CloseStatus.BAD_DATA)
            return
        }
        val gameSession = gameSessionHolder.runningSessions[session.attributes[SessionAttributesEnum.sessionId.value]]
        if (gameSession == null) {
            logger.error { "Invalid Game Session!" }
            session.close(CloseStatus.BAD_DATA)
            return
        }
        val user = gameSession.playerLists[session.attributes[SessionAttributesEnum.userKey.value]];
        if(user == null) {
            logger.error { "Invalid User!!" }
            session.close(CloseStatus.BAD_DATA)
            return
        }
        if(user.isLockedIn)return;
        val notifyDto = gameSession.hoverCharacter(session.attributes[SessionAttributesEnum.userKey.value] as String,requestDto.characterId)
        if(notifyDto == null){
            logger.error { "notifyDto is Null!!!" }; return
        }
        else {
            gameSession.broadcastToTeamPlayer(notifyDto,user.team)
        }
        println("Broadcast PickCharacterOn Message To Players")
    }
    fun handleGetGameTeamPlayerInfo(session: WebSocketSession, dto: WsEventDto){
        val gameSession = gameSessionHolder.runningSessions[session.attributes[SessionAttributesEnum.sessionId.value]]
        val player = gameSession?.playerLists[SessionAttributesEnum.userKey.value]
        if(player == null) {
            logger.error { "Invalid User!!" };
            session.close(CloseStatus.BAD_DATA)
        }
        val sendDto = gameSession?.getInGamePlayerInfo(player!!,gameSession, Session.inGamePlayerlayerInfoType.Team)
        if(sendDto == null) {
            logger.error{ "notifyDto is Null!!!" };  return
        }
        session.sendMessage(
            TextMessage(
                Json.Default.encodeToString(sendDto)
            )
        );
    }
}