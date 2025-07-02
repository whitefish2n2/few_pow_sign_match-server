package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable
import org.springframework.web.socket.WebSocketSession

@Serializable
class Player (
    val id: String,
    val name: String,
    var key:String,
    var matchWebsocket: WebSocketSession?,
){
    var characterId:String? = "";
    var kill:Int = 0;
    var death:Int = 0;
}