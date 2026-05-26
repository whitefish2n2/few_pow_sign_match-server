package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable

@Serializable
data class StartGameDto (
    val gameId:String,
    val sessionVerifyKey:String,
    val sessionIndex:String,
    val url:String,
    val map: String,//map Enum (이름)
    val players:List<AnotherPlayerInfoDto>,
)