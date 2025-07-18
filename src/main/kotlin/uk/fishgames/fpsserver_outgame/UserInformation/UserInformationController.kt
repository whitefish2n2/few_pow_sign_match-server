package uk.fishgames.fpsserver_outgame.UserInformation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/info")
class UserInformationController(
    val userInformationService: UserInformationService,
) {
    @GetMapping("/userstatic")
    fun getUserStatic(id:String): UserPublicStaticInfo?{
        return userInformationService.getUserStaticInfo(id)
    }
}