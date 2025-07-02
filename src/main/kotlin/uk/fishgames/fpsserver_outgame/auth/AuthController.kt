package uk.fishgames.fpsserver_outgame.auth

import jakarta.persistence.NoResultException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.fishgames.fpsserver_outgame.ApiResponse
import uk.fishgames.fpsserver_outgame.ApiResponseCode
import uk.fishgames.fpsserver_outgame.BadDataException
import uk.fishgames.fpsserver_outgame.InvalidJwtException
import uk.fishgames.fpsserver_outgame.auth.dto.RefreshDto
import uk.fishgames.fpsserver_outgame.auth.dto.SignInDto
import uk.fishgames.fpsserver_outgame.auth.dto.SignInResponseDto
import uk.fishgames.fpsserver_outgame.auth.dto.SignInWithRefreshDto
import uk.fishgames.fpsserver_outgame.auth.dto.SignUpDto
import uk.fishgames.fpsserver_outgame.security.JwtUtil

@RestController
@RequestMapping("/auth")
class AuthController(
    private val service: AuthService
) {
    @PostMapping("/signin")
    fun signIn(@RequestBody info: SignInDto): ResponseEntity<ApiResponse<SignInResponseDto>> {
        val res = service.signIn(info)
        return ResponseEntity.ok(ApiResponse<SignInResponseDto>(ApiResponseCode.SignInSuccess.code,"SignIn Request Success.",res))
    }
    @PostMapping("/signin-with-refresh")
    fun signInWithRefreshToken(@RequestBody info: SignInWithRefreshDto):ResponseEntity<ApiResponse<SignInResponseDto>>{
        val res = service.signInWithRefreshToken(info)
        return ResponseEntity.ok(ApiResponse<SignInResponseDto>(
            ApiResponseCode.SignInSuccess.code,
            "SignIn With Refresh Token Request Is Success",
            res))
    }

    //회원가입 후 자동 로그인까지 수행하는 로직(signIn과 같은 dto 사용)
    @PostMapping("/signup")
    fun signUp(@RequestBody info: SignUpDto): ResponseEntity<ApiResponse<SignInResponseDto>> {
        service.signUp(info)

        val r = SignInDto(info.id,info.password)
        val res = service.signIn(r);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse<SignInResponseDto>(ApiResponseCode.SignUpSuccess.code,"Sign Up Request Success. return auto login information",res))
    }
    @GetMapping("/validate-token")
    fun validateToken(@RequestHeader("Authorization") token: String?):ResponseEntity<ApiResponse<Boolean>>{
        println("try validate token: "+ token)

        if(token == null) throw BadDataException()
        service.validateToken(token)//valid하지않으면 throw(핸들러가 핸들 후 error response 전달)
        return ResponseEntity(
            ApiResponse(ApiResponseCode.IsValidJwt.code,"is valid token",true),
            HttpStatus.OK
        )
    }
    @PostMapping("/refresh")
    fun refresh(@RequestBody info: RefreshDto): Any {
        val jwt = service.refreshJwt(info)
        return ResponseEntity.ok().body(ApiResponse<String>(ApiResponseCode.JwtRefreshed.code,"jwt refresh success", jwt))
    }

}