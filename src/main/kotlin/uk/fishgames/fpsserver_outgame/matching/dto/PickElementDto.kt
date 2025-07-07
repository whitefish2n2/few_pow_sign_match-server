package uk.fishgames.fpsserver_outgame.matching.dto

/**
 * 서버 -> Dedicated로 보내는 캐릭터 선택 정보 원자
 * 서버 -> 유저로 보내는 캐릭터 선택 정보 원자
 */
data class PickElementDto (
    val userId:String,
    val characterId:String
)