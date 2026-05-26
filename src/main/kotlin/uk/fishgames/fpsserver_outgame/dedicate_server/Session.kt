package uk.fishgames.fpsserver_outgame.dedicate_server

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import uk.fishgames.fpsserver_outgame.dedicate_server.Session.inGamePlayerlayerInfoType.*
import uk.fishgames.fpsserver_outgame.matching.GameMode
import uk.fishgames.fpsserver_outgame.matching.PickCharacterInformation
import uk.fishgames.fpsserver_outgame.matching.dto.AnotherPlayerInfoDto
import uk.fishgames.fpsserver_outgame.matching.dto.CharacterPickNotifyDto
import uk.fishgames.fpsserver_outgame.matching.dto.MapEnum
import uk.fishgames.fpsserver_outgame.matching.dto.MatchWsEventType
import uk.fishgames.fpsserver_outgame.matching.dto.Player
import uk.fishgames.fpsserver_outgame.matching.dto.hoverCharacterDto
import uk.fishgames.fpsserver_outgame.matching.dto.WsEventDto
import uk.fishgames.fpsserver_outgame.matching.ws.MatchWebsocketRegistry
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger

class Session(val gameId:String, val runningOn:Dedicated,private val matchWebsocketRegister: MatchWebsocketRegistry) {
    private val logger = KotlinLogging.logger {}

    var status: SessionStatus = SessionStatus.Idle
    var mode: GameMode = GameMode.Custom
    var gameMap: MapEnum = MapEnum.Test
    val playerLists: MutableMap<String, Player> = ConcurrentHashMap()//String:User SessionKey
    var score:String = "0:0"
    var playTime:LocalDateTime = LocalDateTime.now()
    val pickFlowInformation: PickCharacterInformation = PickCharacterInformation(Instant.now())
    val pickFlowTimeoutSeconds: Long = 75
    var playerServerConnectKey:String = ""///유저가 데디케이티드 서버로 커넥트할때 사용할 키
    var sessionCreationTask: ScheduledFuture<*>? = null//픽 타임 이후 게임 시작 스케쥴 태스크

    private val lock = ReentrantLock()


    fun init(gameMode: GameMode, gameMap: MapEnum) {
        this.mode = gameMode
        this.playerLists.clear()
        this.gameMap = gameMap
    }
    fun inputPlayer(player: Player) {
        this.playerLists[player.key] = player
    }
    /**
     * GameSessionHolder에 의해 1초에 한번 실행되는 Tick
     */
    fun tick(){
        if(status == SessionStatus.Picking && isPickFlowTimedOut()){
            if(instantlyLockInAllPlayers()){
                StartGamePlayFlow()
            }
            else{
                dodgeGame()
            }
        }
    }
    fun startPickFlow(){
        status = SessionStatus.Picking
    }

    //Tick에 의해서 PickFlow가 시간이 다 끝나고 사람들이 모두 임시로라도 픽을 했으면 시작되거나, 모든 플레이어가 lock in하면 시작된다.
    fun StartGamePlayFlow(){
        if(status == SessionStatus.Picking){
            status = SessionStatus.Playing;
            playTime = LocalDateTime.now()
        }
    }
    fun isPickFlowTimedOut(): Boolean {
        return Duration.between(pickFlowInformation.matchStartedAt, Instant.now()).seconds >= pickFlowTimeoutSeconds
    }
    fun Init(){
        status = SessionStatus.Idle
        for(p in playerLists.values){
            matchWebsocketRegister.get(p.key)?.close()
        }
        playerLists.clear()
        score = ""
    }

    fun dodgeGame(): Boolean{
        if(status == SessionStatus.Playing ){
            logger.warn { "game id: $gameId cannot dodge because its playing!" }
            return false
        }
        sessionCreationTask?.cancel(false)
        sessionCreationTask = null

        broadcastToAllPlayer(WsEventDto(MatchWsEventType.Dodged, Json.encodeToJsonElement("Game Dodged!")))
        return true
    }

    /**
     * 캐릭터 임시 선택(단순 클릭) - 임시 선택의 결과를 알리는WSEventDto를 반환합니다. 핸들러에서 사용해 자신의 팀에 브로드캐스트 할 수 있도록 합니다.
     */
    fun hoverCharacter(sessionUserKey:String, characterId: String): WsEventDto? {
        lock.lock()
        try {
            val p = playerLists[sessionUserKey] ?: return null
            if(p.isLockedIn) return null

            p.characterId = characterId
            val notifyDto = CharacterPickNotifyDto(p.id,
                characterId,
                null/*p.skinList.get[dto.characterId]*/
            )
            return WsEventDto(
                MatchWsEventType.NotifyCharacterChanged,
                Json.encodeToJsonElement(notifyDto)
            )
        } finally {
            lock.unlock()
        }
    }
    /**
     * 캐릭터 확정
     */
    fun pickLockIn(sessionUserKey:String, characterId: String): WsEventDto? {
        lock.lock()//경쟁 상태 차단
        try{
            val p = playerLists[sessionUserKey] ?: return null

            //이미 누가 고른 캐릭터인지 확인
            val isTaken = playerLists.values.any { it.key != sessionUserKey && it.isLockedIn && it.characterId == characterId }
            if (isTaken) return null

            //확정
            p.characterId = characterId
            p.isLockedIn = true
            val notifyDto = CharacterPickNotifyDto(p.id, characterId, null)

            val isAllPlayersLockIn = checkAllPlayersLockedIn()
            if(isAllPlayersLockIn){
                StartGamePlayFlow()
            }
            return WsEventDto(
                MatchWsEventType.NotifyCharacterPicked,
                Json.encodeToJsonElement(notifyDto)
            )
        } finally {
            lock.unlock()
        }

    }
    private fun checkAllPlayersLockedIn():Boolean {
        val isAllPicked = playerLists.values.all { !it.characterId.isNullOrEmpty()  && it.isLockedIn }
        return isAllPicked;
    }

    //모든 플레이어들을 LockIn하고(hover로 선택한 경우) 성공여부를 반환
    private fun instantlyLockInAllPlayers():Boolean {
        lock.lock()//경쟁 상태 차단
        try{
            for(p in playerLists.values){
                if(p.characterId.isNullOrEmpty()) return false;
                if(p.isLockedIn) continue

                //이미 누가 고른 캐릭터인지 확인
                val isTaken = playerLists.any { it.key != p.key && it.value.isLockedIn && it.value.characterId == p.characterId }
                if (isTaken) return false
                val notifyDto = CharacterPickNotifyDto(p.id, p.characterId!!, null)
                val broadCastDto = WsEventDto(
                    MatchWsEventType.NotifyCharacterPicked,
                    Json.encodeToJsonElement(notifyDto)
                )
               p.  isLockedIn = true
                broadcastToAllPlayer(broadCastDto)
            }
            return checkAllPlayersLockedIn()

        } finally {
            lock.unlock() // 예외가 발생해도 반드시 락을 해제하도록 try-finally 권장
        }
    }
    enum class inGamePlayerlayerInfoType {
        Team,
        All,
        Party//미사용
    }

    ///한 플레이어를 기준으로 인게임의 플레이어들을 조회하는 함수
    fun getInGamePlayerInfo(fromPlayer: Player, session: Session, playerInfoType: inGamePlayerlayerInfoType): WsEventDto {
        val notifyDto = mutableListOf<AnotherPlayerInfoDto>()
        when(playerInfoType) {
            Team -> {
                val fromPlayerTeam = fromPlayer.team;
                for(player in session.playerLists.values){
                    if(player.team == fromPlayerTeam){
                        notifyDto.add(AnotherPlayerInfoDto.from(player))
                    }
                }
            }
            All -> {
                for(player in session.playerLists.values){
                    notifyDto.add(AnotherPlayerInfoDto.from(player))
                }
            }

            Party -> {
                val fromPlayerTeam = fromPlayer.team;
                for(player in session.playerLists.values){
                    if(player.team == fromPlayerTeam){
                        notifyDto.add(AnotherPlayerInfoDto.from(player))
                    }
                }
            }
        }
        val broadcastDto = WsEventDto(MatchWsEventType.GameInformation, Json.encodeToJsonElement(notifyDto))
        return broadcastDto
    }
    fun broadcastToAllPlayer(dto: WsEventDto){
        for(p in playerLists.values){
            val soc = matchWebsocketRegister.get(p.key)
            soc?.sendMessage(TextMessage(Json.encodeToString(dto)))
        }
    }
    fun broadcastToTeamPlayer(dto: WsEventDto, team:Int){
        for(player in playerLists.values){
            if(player.team == team){
                val soc = matchWebsocketRegister.get(player.key)
                soc?.sendMessage(TextMessage(Json.encodeToString(dto)))
            }
        }
    }
}
enum class SessionStatus{
    Playing,
    Picking,
    Idle
}