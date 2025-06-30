package uk.fishgames.fpsserver_outgame.dedicate_server

import uk.fishgames.fpsserver_outgame.matching.dto.PlayerDto
import java.time.LocalDateTime

class Session(val gameId:String, val runningOn:Dedicated) {
    var status: SessionStatus = SessionStatus.Idle
    val playerLists: HashMap<String, PlayerDto> = HashMap<String, PlayerDto>();
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
    fun HandleWsMessage(msg:String){
        
    }
}
enum class SessionStatus{
    Playing,
    Idle
}