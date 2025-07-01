package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable
import org.springframework.web.socket.WebSocketSession

@Serializable
class Player (
    val id: String,
    val name: String,
    var key:String,

    var characterId:String,
    var kill:Int,
    var death:Int,
    var matchWebsocket: WebSocketSession?

)