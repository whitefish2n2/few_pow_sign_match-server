package uk.fishgames.fpsserver_outgame.UserInformation

import kotlinx.serialization.Serializable
import uk.fishgames.fpsserver_outgame.UserInformation.Entity.PlayerStaticDataEntity
import uk.fishgames.fpsserver_outgame.auth.Entity.PlayerDataEntity
import java.time.LocalDateTime

@Serializable
data class UserPublicStaticInfo (
    val userId:String,
    val userName:String,

    @Serializable(with = LocalDateTimeAsIso8601::class)
    val createdAt: LocalDateTime
){
    companion object{
        fun from(entity: PlayerStaticDataEntity): UserPublicStaticInfo{
            return UserPublicStaticInfo(entity.userId!!,entity.userName,entity.createdAt)
        }
    }
}