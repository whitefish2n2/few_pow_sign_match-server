package uk.fishgames.fpsserver_outgame.dedicate_server

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import uk.fishgames.fpsserver_outgame.main
import uk.fishgames.fpsserver_outgame.matching.PickCharacterInformation
import uk.fishgames.fpsserver_outgame.matching.dto.CharacterPickNotifyDto
import uk.fishgames.fpsserver_outgame.matching.dto.MatchWsEventType
import uk.fishgames.fpsserver_outgame.matching.dto.Player
import uk.fishgames.fpsserver_outgame.matching.dto.TryCharacterPickDto
import uk.fishgames.fpsserver_outgame.matching.dto.WsEventDto
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

class Session(val gameId:String, val runningOn:Dedicated) {
    var status: SessionStatus = SessionStatus.Idle
    val playerLists: MutableMap<String, Player> = ConcurrentHashMap()//String:User SessionKey
    var score:String = "0:0"
    var playTime:LocalDateTime = LocalDateTime.now()
    val pickFlowInformation: PickCharacterInformation = PickCharacterInformation(Instant.now())
    val pickFlowTimeoutSeconds: Long = 75

    /**
     * GameSessionHolder에 의해 1초에 한번 실행되는 Tick
     */
    fun tick(){
        if(status == SessionStatus.Picking && isPickFlowTimedOut()){
            status = SessionStatus.Playing;
            playTime = LocalDateTime.now()
        }
    }
    fun StartMatchingFlow(){
        status = SessionStatus.Picking
    }
    fun isPickFlowTimedOut(): Boolean {
        return Duration.between(pickFlowInformation.matchStartedAt, LocalDateTime.now()).seconds >= pickFlowTimeoutSeconds
    }
    fun Init(){
        status = SessionStatus.Idle
        for(p in playerLists.values){
            p.matchWebsocket?.close();
        }
        playerLists.clear()
        score = ""
    }

    fun dodgeGame(){
        for (p in playerLists.values){
            if(p.matchWebsocket?.isOpen?:true)
                p.matchWebsocket?.close(CloseStatus.GOING_AWAY);
        }
    }

    /**
     * 캐릭터 임시 선택(단순 클릭)
     */
    fun pickCharacterOn(dto:TryCharacterPickDto): WsEventDto? {
        val p = playerLists.get(dto.sessionUserKey)
        if(p == null) return null
        playerLists[dto.sessionUserKey]?.characterId = dto.characterId
        val notifyDto = CharacterPickNotifyDto(p.id,
            dto.characterId,
            null/*p.skinList.get[dto.characterId]*/
        )

        val notifyEvent = WsEventDto(
            MatchWsEventType.NotifyCharacterChanged,
            Json.encodeToJsonElement(notifyDto)
        )
        return notifyEvent
    }
    /**
     * 캐릭터 확정
     */
    private val lock = ReentrantLock()
    fun pickCharacterUp(dto: TryCharacterPickDto): WsEventDto? {
        lock.lock()//경쟁 상태 차단
        try{
            val p = playerLists[dto.sessionUserKey] ?: return null

            //이미 누가 고른 캐릭터인지 확인
            for(o in playerLists.values){
                if(o.characterId == dto.characterId){
                    return null
                }
            }

            //확정
            p.characterId = dto.characterId
            val notifyDto = CharacterPickNotifyDto(p.id, dto.characterId, null)

            checkAllPlayersLockedIn()

            return WsEventDto(
                MatchWsEventType.NotifyCharacterPicked,
                Json.encodeToJsonElement(notifyDto)
            )
        } finally {
            lock.unlock() // 예외가 발생해도 반드시 락을 해제하도록 try-finally 권장
        }

    }
    private fun checkAllPlayersLockedIn() {
        val isAllPicked = playerLists.values.all { !it.characterId.isNullOrEmpty() }
        if (isAllPicked && status == SessionStatus.Picking) {
            status = SessionStatus.Playing//나중에 pickCharacterUp시점에서 playing인지 확인하고 playing이면 바로시작맨
        }
    }
    fun broadcastToAllPlayer(dto: WsEventDto){
        for(p in playerLists.values){
            p.matchWebsocket?.sendMessage(TextMessage(Json.encodeToString(dto)))
        }
    }
}
enum class SessionStatus{
    Playing,
    Picking,
    Idle
}