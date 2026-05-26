package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import uk.fishgames.fpsserver_outgame.UserInformation.UserDynamicInfo
import uk.fishgames.fpsserver_outgame.UserInformation.UserPublicStaticInfo

@Serializable
class Player (
    val id: String,
    val name: String,
    var key:String,
    var staticInfo: UserPublicStaticInfo,
){
    var characterId:String? = "";
    var team:Int = 0;//0なら個人チーム
    @Transient
    var isLockedIn = false;
    var dynamicInfo: UserDynamicInfo = UserDynamicInfo();
}