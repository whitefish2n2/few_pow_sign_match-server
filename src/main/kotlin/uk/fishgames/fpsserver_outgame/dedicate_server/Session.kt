package uk.fishgames.fpsserver_outgame.dedicate_server

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.springframework.web.socket.TextMessage
import uk.fishgames.fpsserver_outgame.matching.dto.CharacterPickNotifyDto
import uk.fishgames.fpsserver_outgame.matching.dto.MatchWsEventType
import uk.fishgames.fpsserver_outgame.matching.dto.Player
import uk.fishgames.fpsserver_outgame.matching.dto.TryCharacterPickDto
import uk.fishgames.fpsserver_outgame.matching.dto.WsEventDto
import java.time.LocalDateTime
import java.util.concurrent.locks.ReentrantLock

class Session(val gameId:String, val runningOn:Dedicated) {
    var status: SessionStatus = SessionStatus.Idle
    val playerLists: HashMap<String, Player> = HashMap<String, Player>();//String:User SessionKey
    var score:String = "0:0"
    var playTime:LocalDateTime = LocalDateTime.now()
    fun Init(){
        status = SessionStatus.Idle
        for(p in playerLists.values){
            p.matchWebsocket?.close();
        }
        playerLists.clear()
        score = ""
        playTime = LocalDateTime.now()
    }

    /**
     * 캐릭터 임시 선택(단순 클릭)
     */
    fun pickCharacterTemporary(dto:TryCharacterPickDto): WsEventDto? {
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
    fun pickCharacter(dto: TryCharacterPickDto): WsEventDto? {
        lock.lock()//경쟁 상태 차단

        val p = playerLists.get(dto.sessionUserKey)
        if(p == null) return null
        for(o in playerLists.values){
            if(o.characterId == dto.characterId){
                return null
            }
        }
        playerLists[dto.sessionUserKey]?.characterId = dto.characterId
        val notifyDto = CharacterPickNotifyDto(p.id,
            dto.characterId,
            null/*p.skinList.get[dto.characterId]*/
        )

        lock.unlock()
        val notifyEvent = WsEventDto(
            MatchWsEventType.NotifyCharacterPicked,
            Json.encodeToJsonElement(notifyDto)
        )
        return notifyEvent
    }

    fun broadcastToAllPlayer(dto: WsEventDto){
        for(p in playerLists.values){
            p.matchWebsocket?.sendMessage(TextMessage(Json.encodeToString(dto)))
        }
    }
}
enum class SessionStatus{
    Playing,
    Idle
}