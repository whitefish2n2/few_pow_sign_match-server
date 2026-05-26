package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable

@Serializable
data class MatchFoundDto (
    val gameId: String,
    val gameMode: String, // 모드 명 (enum name)
    val teamInfo: Int,    // 0: Blue, 1: Red 등
    val map: String,      // 맵 이름(enum name)
    val teamPlayers: List<AnotherPlayerInfoDto>, // 아군 정보만 전송
    val pickEndTime: Long // 픽 마감 시간
)