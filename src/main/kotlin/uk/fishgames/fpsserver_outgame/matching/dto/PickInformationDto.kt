package uk.fishgames.fpsserver_outgame.matching.dto

data class PickInformationDto (
    val remainingTime:Int,
    val pickUpInfo:List<PickElementDto>,
    val pickOnInfo:List<PickElementDto>,
)