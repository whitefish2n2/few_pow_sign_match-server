package uk.fishgames.fpsserver_outgame.dedicate_server

import uk.fishgames.fpsserver_outgame.matching.dto.PlayerDto
import java.time.LocalDateTime

class Session(val gameId:String,val runningOn:Dedicated) {
    var status: ServerStatus = ServerStatus.Idle
    val playerLists = ArrayList<PlayerDto>()
    var score:String = "0:0"
    var playTime:LocalDateTime = LocalDateTime.now()
    fun Init(){
        status = ServerStatus.Idle
        playerLists.clear()
        score = ""
        playTime = LocalDateTime.now()
    }
}