package uk.fishgames.fpsserver_outgame.UserInformation

import org.springframework.stereotype.Service
import uk.fishgames.fpsserver_outgame.PlayerNotFoundException
import uk.fishgames.fpsserver_outgame.UserInformation.repo.PlayerStaticDataRepository

@Service
class UserInformationService(
    val playerStaticDataRepository: PlayerStaticDataRepository,
) {

    fun initProfile(){

    }

    fun getUserStaticInfo(userId:String): UserPublicStaticInfo {
        val entity = playerStaticDataRepository.findByUserId(userId)
        if(entity.isEmpty) throw PlayerNotFoundException()
        val e = entity.get()
        if(e.userId==null) throw PlayerNotFoundException()
        return UserPublicStaticInfo(e.userId!!,e.userName,e.createdAt);
    }
}