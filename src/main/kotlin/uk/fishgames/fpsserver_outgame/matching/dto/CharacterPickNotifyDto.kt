package uk.fishgames.fpsserver_outgame.matching.dto

/**
 * WebsocketDto
 * 서버->유저로 전달하는 캐릭터 선택 정보
 */
data class CharacterPickNotifyDto (
    val playerId:String,
    val characterId:String,
    val skinId:String?,
)