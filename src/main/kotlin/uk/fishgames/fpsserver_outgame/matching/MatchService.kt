package uk.fishgames.fpsserver_outgame.matching

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.springframework.http.MediaType
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import uk.fishgames.fpsserver_outgame.FishUtil
import uk.fishgames.fpsserver_outgame.PlayerNotFoundException
import uk.fishgames.fpsserver_outgame.UserInformation.UserPublicStaticInfo
import uk.fishgames.fpsserver_outgame.UserInformation.repo.PlayerStaticDataRepository
import uk.fishgames.fpsserver_outgame.auth.repo.PlayerRepository
import uk.fishgames.fpsserver_outgame.dedicate_server.Dedicated
import uk.fishgames.fpsserver_outgame.dedicate_server.Session
import uk.fishgames.fpsserver_outgame.dedicate_server.SessionStatus
import uk.fishgames.fpsserver_outgame.dedicatedClients
import uk.fishgames.fpsserver_outgame.matching.dto.*
import uk.fishgames.fpsserver_outgame.matching.ws.MatchWebsocketRegistry
import uk.fishgames.fpsserver_outgame.security.JwtUtil
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.map
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaInstant
@EnableScheduling
@Service
class MatchService(
    private val jwtUtil: JwtUtil,
    private val playerRepository: PlayerRepository,
    private val playerStaticRepository: PlayerStaticDataRepository,
    private val matchQueueManager: MatchQueueManager,
    private val webClient: WebClient,
    private val matchWebsocketRegister: MatchWebsocketRegistry,
    private val taskScheduler: TaskScheduler,
    private val gameSessionHolder: GameSessionHolder
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
            val newPlayer = Player(player.id,playerStatic.userName, FishUtil.randomUUID(), UserPublicStaticInfo.from(playerStatic));
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
     * @return 등록 성공 여부 (Boolean)
     * tryMateMatch(mode) 실행
     * matchWebsocketRegister에 userId기반으로 등록
     * matchQueueManager에 등록
     */
    fun registerPlayer(session: WebSocketSession, player: Player, mode: GameMode): Boolean {
        logger.info { "Registering new player ${player.id}, Mode:$mode" }

        matchQueueManager.enqueue(mode,player.key,player)
        return true
    }


    fun cancelPlayer(session: WebSocketSession) {
        val playerId = session.attributes.get(SessionAttributesEnum.userId.value) as? String?:return
        val userKey = session.attributes[SessionAttributesEnum.userKey.value] as? String ?: return

        matchQueueManager.cancel(userKey)
        if(session.attributes.get(SessionAttributesEnum.sessionId.value) != null) {
            val gameSession= gameSessionHolder.runningSessions[session.attributes[SessionAttributesEnum.sessionId.value]];
            if(gameSession?.status != SessionStatus.Playing){
                    gameSession?.dodgeGame();
                    logger.info { "Game Session has been cancelled. caused by player match anomaly: ${session.attributes.get(SessionAttributesEnum.userId.value)}" }
                }
            }

        println("clearing player: $playerId")
    }

    fun makeSessionOnDedicatedServer(session: Session) {

        val players = session.playerLists.values;
        val newPlayers = players.map { p ->
            DedicatedNewPlayerDto(p.id, p.name, p.key, p.team, p.characterId)
        }

        val gameSetupBoddari = GameSetupBoddari(
            session.gameId,
            newPlayers,
            session.mode,
            session.gameMap.ordinal,//서버->데디케이티드는 id로 전송
        )
        logger.info { "try make session to ${session.runningOn.httpUrl}/makesession" }
        logger.info { gameSetupBoddari.toString() }

        val body = Json.encodeToString(gameSetupBoddari)

        logger.debug { body }
        val res = webClient.post()
            .uri("${session.runningOn.httpUrl}/makesession")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String::class.java)
            .doOnSuccess { res ->//res : 서버의 session key(uint16) toString 값
                println("Dedi Server Session Key: $res")
                val hasDisconnectedPlayer = players.any { p ->
                    matchWebsocketRegister.get(p.key)?.isOpen == false
                }

                // 끊긴 유저가 있다면 닷지 처리 후 바로 종료 (throw 대신 return)
                if (hasDisconnectedPlayer) {
                    logger.warn { "유저 연결 끊김 감지됨. 게임을 닷지합니다. GameID: ${session.gameId}" }
                    session.dodgeGame()
                    return@doOnSuccess
                }

                val data = StartGameDto(
                    session.gameId,
                    res,
                    session.runningOn.ip,
                    session.runningOn.udpPort,
                    session.gameMap.name,
                    players.map { p: Player -> AnotherPlayerInfoDto.from(p) })
                val playerNotifyDto = Json.encodeToString(
                    WsEventDto(
                        MatchWsEventType.StartMatch,
                        Json.encodeToJsonElement(data)
                    )
                )
                session.status = SessionStatus.Playing
                for (p in players) {
                    val playerWs = matchWebsocketRegister.get(p.key);
                    playerWs?.sendMessage(TextMessage(playerNotifyDto))
                    matchQueueManager.cancel(p.key);
                    playerWs?.close(CustomWebsocketCloseCode.GAME_STARTED)
                }
            }
            .doOnError {
                logger.error(it) { "Error while making session" }
                logger.error { " ${it.message}" }
                session.dodgeGame()
            }
            .subscribe()
    }

    @OptIn(ExperimentalTime::class)
    fun tryMakeMatch(mode: GameMode): Any? {
        val random = Random(TimeUnit.MICROSECONDS.toSeconds(Random.nextLong()))
        val target = getDediServer()?:return null

        val players = matchQueueManager.makeMatch(mode) ?: return null
        val map = MapEnum.entries[random.nextInt(1,MapEnum.entries.size)]//랜덤 맵 지정이에요 todo: 모드에 따른 맵 풀 시스템 제작
        val gameId = LocalDateTime.now().toString() + FishUtil.randomUUID()//랜덤 게임 id 생성이에요
        val newSession = Session(gameId,target,matchWebsocketRegister)
        gameSessionHolder.putSession(newSession)
        newSession.init(mode,map)
        for(player in players) {
            newSession.inputPlayer(player)
        }

        newSession.onGameStartReady = { readySession ->
            makeSessionOnDedicatedServer(readySession)
        }
        newSession.startPickFlow()

        // 이미 배정된 player.team 값을 기준으로 플레이어들을 그룹화
        val playersByTeam = players.groupBy { it.team }

        //픽 마감 시간 계산 (현재 시간 밀리초 + 타임아웃 초 단위 * 1000)
        val pickEndTimeMillis = Instant.now().toEpochMilli() + (newSession.pickFlowTimeoutSeconds * 1000L)

        //플레이어 개별 순회하며 본인 팀 정보만 전송
        for (p in players) {
            // 본인과 같은 팀 번호를 가진 플레이어 리스트를 가져와서 ClientNewPlayerDto로 변환
            val sameTeamPlayers = playersByTeam[p.team] ?: emptyList()
            val teamDtoList = sameTeamPlayers.map { AnotherPlayerInfoDto.from(it) }

            val data = MatchFoundDto(
                gameId = newSession.gameId,
                gameMode = mode.name,
                teamInfo = p.team,        // 본인의 팀 번호 전달
                map = map.name,
                teamPlayers = teamDtoList, // 자신이 속한 팀의 플레이어들만 포함
                pickEndTime = pickEndTimeMillis
            )

            val playerNotifyDto = Json.encodeToString(
                WsEventDto(
                    MatchWsEventType.MatchFound,
                    Json.encodeToJsonElement(data)
                )
            )

            val playerWs = matchWebsocketRegister.get(p.key)
            playerWs?.sendMessage(TextMessage(playerNotifyDto))
            matchQueueManager.cancel(p.key)
            matchWebsocketRegister.get(p.key)?.attributes?.set(SessionAttributesEnum.sessionId.value, newSession.gameId)

        }
        val delaySeconds = newSession.pickFlowTimeoutSeconds
        val executeTime = (Clock.System.now() + delaySeconds.seconds).toJavaInstant()

        logger.info { "${delaySeconds}초 뒤 세션 생성 스케줄링 등록 완료. GameID: ${newSession.gameId}" }

        newSession.sessionCreationTask = taskScheduler.schedule({
            try {
                if (newSession.status == SessionStatus.Picking) {
                    logger.info { "픽 타임 강제 종료 (75초 도달). 남은 유저 강제 픽 진행." }

                    if(newSession.instantlyLockInAllPlayers()) {
                        newSession.StartGamePlayFlow()
                    } else {
                        newSession.dodgeGame()
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "스케줄링된 세션 생성 작업 실패" }
            }
        }, executeTime)

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



    private fun extractPlayerId(session: WebSocketSession): String {
        return session.attributes["userId"].toString()
    }
}

