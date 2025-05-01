package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable

@Serializable
data class NewPlayerDto (
    val id: String,
    val name: String,
)