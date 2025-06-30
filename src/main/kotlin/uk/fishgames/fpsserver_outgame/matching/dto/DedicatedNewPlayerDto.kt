package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable

//여기->데디케이티드서버로 전달하는 플레이어 객체 dto
@Serializable
data class DedicatedNewPlayerDto (
    val id: String,
    val name: String,
    val key:String,

){
    companion object{
        fun from(dto: PlayerDto): DedicatedNewPlayerDto{
            return DedicatedNewPlayerDto(dto.id,dto.name,dto.key)
        }
    }
}