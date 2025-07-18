package uk.fishgames.fpsserver_outgame.matching

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import uk.fishgames.fpsserver_outgame.FishUtil
import uk.fishgames.fpsserver_outgame.PlayerNotFoundException
import uk.fishgames.fpsserver_outgame.UserInformation.UserPublicStaticInfo
import uk.fishgames.fpsserver_outgame.UserInformation.repo.PlayerStaticDataRepository
import uk.fishgames.fpsserver_outgame.auth.repo.PlayerRepository
import uk.fishgames.fpsserver_outgame.dedicate_server.Dedicated
import uk.fishgames.fpsserver_outgame.dedicate_server.Session
import uk.fishgames.fpsserver_outgame.dedicatedClients
import uk.fishgames.fpsserver_outgame.matching.dto.*
import uk.fishgames.fpsserver_outgame.security.JwtUtil
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.map
import kotlin.random.Random

@Service
class MatchService(
    private val jwtUtil: JwtUtil,
    private val playerRepository: PlayerRepository,
    private val playerStaticRepository: PlayerStaticDataRepository,
    private val matchQueueManager: MatchQueueManager,
    private val webClient: WebClient,
    private val matchWebsocketRegister: MatchWebsocketRegistry
) {
    private val logger = KotlinLogging.logger {}

    /**
     * @param id : 유저의 id
     * @return PlayerDto? - id를 기반으로 db에서 매치에 필요한 유저 정보를 조회 후 유저 정보를 반환, 조회 실패시 null 반환
     * */
    fun createPlayerDtoFromDataBase(id: String, webSocketSession: WebSocketSession): Player? {
        try {
            val p = playerRepository.findById(id)
            val ps = playerStaticRepository.findById(id)
            if(p.isEmpty || ps.isEmpty) return null
            val player = p.get()
            val playerStatic = ps.get()
            val newPlayer = Player(player.id,playerStatic.userName, FishUtil.randomUUID(), webSocketSession);
            newPlayer.staticInfo = UserPublicStaticInfo.from(playerStatic);
            return newPlayer;
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
    fun registerPlayer(session: WebSocketSession, player: Player, mode: GameMode): Boolean {
        logger.info { "Registering new player ${player.id}, Mode:$mode" }

        matchWebsocketRegister.register(player.key,session)
        matchQueueManager.enqueue(mode,player.key,player)
        return true;
    }


    fun cancelPlayer(session: WebSocketSession) {
        val playerId = session.attributes.get(SessionAttributesEnum.userId.value) as String
        val userKey = session.attributes.get(SessionAttributesEnum.userKey.value) as String

        matchWebsocketRegister.get(userKey)?.close(CloseStatus.NORMAL)
        matchWebsocketRegister.remove(userKey)
        matchQueueManager.cancel(userKey)

        println("WebSocket disconnected: $playerId")
    }

    fun tryMakeMatch(mode: GameMode): Any? {
        val random = Random(TimeUnit.MICROSECONDS.toSeconds(Random.nextLong()))
        val target = getDediServer()?:return null

        val Players = matchQueueManager.makeMatch(mode) ?: return null
        val newPlayers:List<DedicatedNewPlayerDto> = Players.map { p: Player-> DedicatedNewPlayerDto.from(p) }


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
                    println(res)
                    val newSession = Session(
                        gameId = gameId,
                        runningOn = target,
                    )
                    val data = MatchFoundDto(gameId, connectKey
                        ,res, newSession.runningOn.serverUrl, map, Players.map { p: Player-> ClientNewPlayerDto.from(p) })
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
                        matchQueueManager.cancel(p.key);
                    }
                    target.session.add(newSession)
                    GameSessionHolder.putSession(newSession)
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


    var rrIndex: AtomicInteger = AtomicInteger(0)
    fun getDediServer(): Dedicated? {
        try {
            logger.info { "try find dedi rrIndex: $rrIndex" }
            if(dedicatedClients.entries.size==0) return null
            val v =dedicatedClients.entries.elementAt(
                rrIndex.get()).value
            rrIndex.set((rrIndex.addAndGet(1)) % dedicatedClients.count())
            return v
        }
        catch(ex:Exception) {
            logger.error { ex }
            return null
        }
    }

    /**
     * 서버 단 미구현 * 엔드포인트 미존재
     * 닷지됐을때 서버 정리를 위한 함수
     */
    fun dodgeGame(target:Dedicated, gameId: String){
        try {
            webClient.post()
                .uri("${target.serverUrl}/dodgeGame")
                .bodyValue(gameId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String::class.java)
                .doOnError {
                    logger.error(it) { "Error while making session" }
                    throw it
                }
                .subscribe()
        }
        catch (ex:Exception) {
            logger.error(ex) { "Error while making session" }
        }
    }


    private fun extractPlayerId(session: WebSocketSession): String {
        return session.attributes["userId"].toString()
    }
}
