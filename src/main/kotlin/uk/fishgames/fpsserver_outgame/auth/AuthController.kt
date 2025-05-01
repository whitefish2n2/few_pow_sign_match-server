package uk.fishgames.fpsserver_outgame.auth

import jakarta.persistence.NoResultException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.fishgames.fpsserver_outgame.auth.dto.RefreshDto
import uk.fishgames.fpsserver_outgame.auth.dto.SignInDto
import uk.fishgames.fpsserver_outgame.auth.dto.SignInResponseDto
import uk.fishgames.fpsserver_outgame.auth.dto.SignUpDto

@RestController
@RequestMapping("/auth")
class AuthController(
    private val service: AuthService
) {
    @PostMapping("/signin")
    fun signIn(@RequestBody info: SignInDto): Any {
        val res = service.signIn(info)
        return ResponseEntity.ok(res)
    }

    @PostMapping("/signup")
    fun signUp(@RequestBody info: SignUpDto): Any {
        service.signUp(info)
        return ResponseEntity.status(HttpStatus.CREATED).body("계정 생성이 성공적으로 완료되었습니다.")
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody info: RefreshDto): Any {
        val jwt = service.refreshJwt(info)
        return ResponseEntity.ok().body(jwt)
    }
}