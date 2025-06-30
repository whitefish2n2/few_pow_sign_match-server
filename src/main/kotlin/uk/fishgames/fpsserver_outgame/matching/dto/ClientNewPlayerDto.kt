package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable



///서버->유저에게 전달하는 새로운 플레이어 dto
@Serializable
class ClientNewPlayerDto(
    val id: String,
    val name: String,

) {
    companion object {
        fun from(dto:PlayerDto): ClientNewPlayerDto {
            return ClientNewPlayerDto(dto.id,dto.name);
        }
    }
}