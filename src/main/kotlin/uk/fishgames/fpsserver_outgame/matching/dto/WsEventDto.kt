package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class WsEventDto(
    val Type: MatchWsEventType,
    val Message: JsonElement?,
){
    companion object {
        fun ensureEnqueue(dto: EnsureMatchDto) : String{
            return Json.encodeToString(
                WsEventDto(MatchWsEventType.EnsureEnqueueMatch, Json.encodeToJsonElement(dto))
        )
        }
        val pong: String = Json.encodeToString(
            WsEventDto(MatchWsEventType.Pong, Json.encodeToJsonElement("Pong"))
        )
    }
}