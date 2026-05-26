package uk.fishgames.fpsserver_outgame.auth

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.fishgames.fpsserver_outgame.ApiResponse
import uk.fishgames.fpsserver_outgame.ApiResponseCode
import uk.fishgames.fpsserver_outgame.UserInformation.PlayerInformationService
import uk.fishgames.fpsserver_outgame.auth.dto.PlayerPrivateInformationDto
import java.security.Principal

@RestController
@RequestMapping("/user")
class PlayerInformationController(
    private val playerInformationService: PlayerInformationService
) {

    @GetMapping("/getCurrentPlayerInformation")
    fun getCurrentPlayerInformation(principal: Principal): ResponseEntity<ApiResponse<PlayerPrivateInformationDto>> {
        val userId = principal.name

        val playerInfo = playerInformationService.getCurrentPlayerInformation(userId)

        return ResponseEntity.ok(
            ApiResponse<PlayerPrivateInformationDto>(
                ApiResponseCode.Default.code,
                "Get Player Information Success.",
                playerInfo
            )
        )
    }
}