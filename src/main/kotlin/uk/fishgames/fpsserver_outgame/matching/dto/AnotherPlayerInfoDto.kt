package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import uk.fishgames.fpsserver_outgame.UserInformation.UserDynamicInfo
import uk.fishgames.fpsserver_outgame.UserInformation.UserPublicStaticInfo


//한 플레이어에게 다른 플레이어의 정보를 주기 위한 DTO, 비민감한 정보만 포함
@Serializable
class AnotherPlayerInfoDto (
    val id: String,
    val name: String,
    var characterId:String?,
    var team:Int = 0,
    var kill:Int = 0,
    var death:Int = 0,
    var isLockedIn:Boolean = false,
    var staticInfo: UserPublicStaticInfo,
    var dynamicInfo: UserDynamicInfo
){
    companion object {
        fun from(player: Player): AnotherPlayerInfoDto {
            return AnotherPlayerInfoDto(
                id = player.id,
                name = player.name,
                characterId = player.characterId,
                team = player.team,
                staticInfo = player.staticInfo,
                isLockedIn = player.isLockedIn,
                dynamicInfo = player.dynamicInfo
            )
        }
    }
}