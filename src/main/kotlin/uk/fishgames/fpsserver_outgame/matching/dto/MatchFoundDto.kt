package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable

@Serializable
data class MatchFoundDto (
    val gameId:String,
    val sessionConnectKey:String,
    val url:String,
    val map: MapEnum,
    val players:List<NewPlayerDto>,
)