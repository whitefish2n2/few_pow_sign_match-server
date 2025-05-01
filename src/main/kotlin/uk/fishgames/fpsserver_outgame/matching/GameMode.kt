package uk.fishgames.fpsserver_outgame.matching

import kotlinx.serialization.Serializable
import uk.fishgames.fpsserver_outgame.dedicate_server.Dedicated

@Serializable
enum class GameMode(val id:Int,val count:Int){
    DeathMatch(0,10),
    OneVsOne(1,2),
    Solo(2,1),
    Custom(-1,-1);
    companion object {
        fun fromId(id: Int?): GameMode? = entries.find { it.id == id }
    }
}