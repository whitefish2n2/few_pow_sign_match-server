package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable
import org.springframework.web.socket.WebSocketSession
import uk.fishgames.fpsserver_outgame.UserInformation.UserDynamicInfo
import uk.fishgames.fpsserver_outgame.UserInformation.UserPublicStaticInfo

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
    var staticInfo: UserPublicStaticInfo? = null;
    var dynamicInfo: UserDynamicInfo = UserDynamicInfo();
}