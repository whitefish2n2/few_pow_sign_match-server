package uk.fishgames.fpsserver_outgame.matching.dto

/**
 * 서버->데디서버로 보내는 캐릭터 선택 정보
 */
data class CharacterSetDto(
    val sessionId: String,
    val elements: List<PickElementDto>,
)