package uk.fishgames.fpsserver_outgame.auth.dto

    data class signInResponseDto (
        val jwt:String,
        val refreshToken: String,
    )