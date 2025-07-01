package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable


/**서버->유저에게 전달하는 새로운 플레이어 dto
 * 유저 티어, 유저 아이콘 등의 정보 포함 가능
 * 유저 key 등 personal한 정보는 포함 X
 */
@Serializable
class ClientNewPlayerDto(
    val id: String,
    val name: String,

) {
    companion object {
        fun from(p:Player): ClientNewPlayerDto {
            return ClientNewPlayerDto(p.id,p.name);
        }
    }
}