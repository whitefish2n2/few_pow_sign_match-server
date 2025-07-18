package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable

@Serializable
data class TryCharacterPickDto(
    val sessionId:String,
    val sessionUserKey:String,
    val characterId:String,
)