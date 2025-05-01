package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Serializable
data class WsEventDto(
    val Type: String,
    val Message: String
){
    fun typeEnum(): WsEventType = WsEventType.valueOf(Type)
}