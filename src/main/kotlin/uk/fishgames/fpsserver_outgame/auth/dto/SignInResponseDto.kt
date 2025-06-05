package uk.fishgames.fpsserver_outgame.auth.dto

    data class SignInResponseDto (
        val jwt:String,
        val refreshToken: String,
    )