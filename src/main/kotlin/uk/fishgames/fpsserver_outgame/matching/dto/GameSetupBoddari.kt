package uk.fishgames.fpsserver_outgame.matching.dto

import kotlinx.serialization.Serializable
import uk.fishgames.fpsserver_outgame.matching.GameMode

//id 제외 string attribute들은 나중에 enum으로 바꿀 필요가 있는 애들임

@Serializable
data class GameSetupBoddari(
    val gameId:String,
    val players:List<DedicatedNewPlayerDto>,
    val gameMode:GameMode,
    val map: MapEnum,//TODO: 이거 직렬화 제대로 되는지 검증할필요있음
    val playerConnectKey:String,
)