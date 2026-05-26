package uk.fishgames.fpsserver_outgame.auth.dto

data class PlayerPrivateInformationDto (
    val userId: String,
    val userName: String,
    val createdAt:Long,
)