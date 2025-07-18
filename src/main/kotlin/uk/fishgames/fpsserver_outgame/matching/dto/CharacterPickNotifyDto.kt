package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable

/**
 * WebsocketDto
 * 서버->유저로 전달하는 캐릭터 선택 정보
 */
@Serializable
data class CharacterPickNotifyDto (
    val playerId:String,
    val characterId:String,
    val skinId:String?,
)