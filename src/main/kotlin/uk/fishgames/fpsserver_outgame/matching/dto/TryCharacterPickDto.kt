package uk.fishgames.fpsserver_outgame.matching.dto

data class TryCharacterPickDto(
    val sessionId:String,
    val sessionUserKey:String,
    val characterId:String,
)