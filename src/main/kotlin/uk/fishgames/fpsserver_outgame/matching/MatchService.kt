package uk.fishgames.fpsserver_outgame.matching

import kotlinx.serialization.json.Json
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.fishgames.fpsserver_outgame.FishUtil
import uk.fishgames.fpsserver_outgame.PlayerNotFoundException
import uk.fishgames.fpsserver_outgame.auth.repo.PlayerRepository
import uk.fishgames.fpsserver_outgame.dedicate_server.Dedicated
import uk.fishgames.fpsserver_outgame.dedicate_server.Session
import uk.fishgames.fpsserver_outgame.dedicatedClients
import uk.fishgames.fpsserver_outgame.matching.dto.GameSetupBoddari
import uk.fishgames.fpsserver_outgame.security.JwtUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.encodeToJsonElement
import org.hibernate.annotations.CurrentTimestamp
import org.springframework.cglib.core.Local
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import uk.fishgames.fpsserver_outgame.matching.dto.MapEnum
import uk.fishgames.fpsserver_outgame.matching.dto.MatchFoundDto
import uk.fishgames.fpsserver_outgame.matching.dto.DedicatedNewPlayerDto
import uk.fishgames.fpsserver_outgame.matching.dto.EnsureMatchDto
import uk.fishgames.fpsserver_outgame.matching.dto.PlayerDto
import uk.fishgames.fpsserver_outgame.matching.dto.WsEventDto
import uk.fishgames.fpsserver_outgame.matching.dto.MatchWsEventType
import uk.fishgames.fpsserver_outgame.matching.dto.SessionAttributesEnum
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@Service
class MatchService(
    private val jwtUtil: JwtUtil,
    private val playerRepository: PlayerRepository,
    private val matchQueueManager: MatchQueueManager,
    private val webClient: WebClient,
    private val matchWebsocketRegister: MatchWebsocketRegistry,
    private val gameSessionHolder: GameSessionHolder
) {
    private val logger = KotlinLogging.logger {}

    /**
     * @param id : 유저의 id
     * @return PlayerDto? - id를 기반으로 db에서 매치에 필요한 유저 정보를 조회 후 유저 정보를 반환, 조회 실패시 null 반환
     * */
    fun CreatePlayerDtoFromDataBase(id: String, webSocketSession: WebSocketSession): PlayerDto? {
        try {
            val p = playerRepository.findById(id)
            if(p.isEmpty) return null
            val player = p.get()
            return PlayerDto(player.id,player.name, FishUtil.uuid(player.name),0,0, webSocketSession)
        }
        catch(ex: Exception) {
            logger.info { "Error while trying to get player $id" }
            println(ex)
            throw PlayerNotFoundException()
        }
    }


    /**
     * 플레이어를 대기열에 등록하는 함수입니다
     * @param session:웹소켓 세션
     *
     * @suppress session의 attributes의 userId, gameMode가 채워져있어야함
     * @return 등록 성공 여부 (Boolean)
     * tryMateMatch(mode) 실행
     * matchWebsocketRegister에 userId기반으로 등록
     * matchQueueManager에 등록
     */
    fun registerPlayer(session: WebSocketSession, dto: PlayerDto, mode: GameMode): Boolean {
        logger.info { "Registering new player ${dto.id}, Mode:$mode" }

        matchWebsocketRegister.register(dto.key,session)
        matchQueueManager.enqueue(mode,dto.key,dto)

        session.sendMessage(TextMessage(WsEventDto.ensureEnqueue(EnsureMatchDto(dto.key))))
        tryMakeMatch(mode)
        return true;
    }


    fun cancelPlayer(session: WebSocketSession, status: CloseStatus) {
        val playerId = extractPlayerId(session)
        matchWebsocketRegister.remove(playerId)
        matchQueueManager.cancel(playerId)
        println("WebSocket disconnected: $playerId")
    }


    val gameIdSalt:Int = 0;
    fun tryMakeMatch(mode: GameMode): Any? {
        val random = Random(TimeUnit.MICROSECONDS.toSeconds(Random.nextLong()))
        val target = getDediServer()?:return null

        val Players = matchQueueManager.makeMatch(mode) ?: return null
        val newPlayers:List<DedicatedNewPlayerDto> = Players.map { p: PlayerDto-> DedicatedNewPlayerDto.from(p) }


        val map = MapEnum.entries.get(random.nextInt(0,MapEnum.entries.size))//랜덤 맵 지정이에요

        val gameId = LocalDateTime.now().toString() + FishUtil.randomUUID()//랜덤 게임 id 생성이에요

        val connectKey = FishUtil.hash(FishUtil.uuid(gameId))//랜덤 클라이언트->서버 커넥트 키 생성이에요

        val gameSetupBoddari = GameSetupBoddari(gameId,newPlayers,mode, map, connectKey)
        logger.info { "try make session to ${target.serverUrl}/makesession" }
        logger.info { gameSetupBoddari.toString() }

        try {

            val body = Json.encodeToString(gameSetupBoddari)

            logger.debug { body }

            val res = webClient.post()
                .uri("${target.serverUrl}/makesession")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String::class.java)
                .doOnSuccess{res->//res : 서버의 session key(uint16) toString 값
                    val newSession = Session(
                        gameId = gameId,
                        runningOn = target,
                    )
                    val data = MatchFoundDto(gameId, connectKey
                        ,res, newSession.runningOn.serverUrl, map, newPlayers)
                    val playerNotifyDto = Json.encodeToString(
                        WsEventDto(
                            MatchWsEventType.MatchFound,
                            Json.encodeToJsonElement(data))
                    )
                    for(p in Players){

                        val playerWs = matchWebsocketRegister.get(p.key);


                        if (playerWs?.isOpen == true)
                        {
                            println("containing ${p.id}")
                            newSession.playerLists.put(p.key, p)
                            println(playerWs.attributes[SessionAttributesEnum.userId.value])

                        }
                        else {

                            throw PlayerNotFoundException()
                        }
                    }
                    for(p in Players){
                        val playerWs = matchWebsocketRegister.get(p.key);
                        playerWs?.sendMessage(TextMessage(playerNotifyDto))
                        playerWs?.attributes?.set("sessionId", gameId)
                        matchWebsocketRegister.remove(p.key);
                        matchQueueManager.cancel(p.key);
                    }
                    target.session.add(newSession)
                    gameSessionHolder.putSession(newSession)
                }
                .doOnError {
                    logger.error(it) { "Error while making session" }
                    throw it
                }
                .subscribe()

        } catch (e: WebClientResponseException) {
            println("Match creation failed: ${e.statusCode} - ${e.responseBodyAsString}")
            return null
        }
        catch (e:Exception){
            logger.info{ e.toString() }
            logger.error(e) { "Error while making session" }

            return null
        }
        return true;
    }


    var rrIndex:Int = 0
    fun getDediServer(): Dedicated? {
        try {
            logger.info { "try find dedi rrIndex: $rrIndex" }
            if(dedicatedClients.entries.size==0) return null
            val v =dedicatedClients.entries.elementAt(
                rrIndex).value
            rrIndex = (rrIndex + 1) % dedicatedClients.count()
            return v
        }
        catch(ex:Exception) {
            logger.error { ex }
            return null
        }
    }


    private fun extractPlayerId(session: WebSocketSession): String {
        return session.attributes["userId"].toString()
    }
}
