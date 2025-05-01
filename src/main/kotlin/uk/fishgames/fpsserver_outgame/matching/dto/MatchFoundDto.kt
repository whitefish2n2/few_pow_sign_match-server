package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable

@Serializable
data class MatchFoundDto (
    val gameId:String,
    val url:String,
    val map:String,//todo: 얘 enum화하셈
    val players:List<NewPlayerDto>,
)