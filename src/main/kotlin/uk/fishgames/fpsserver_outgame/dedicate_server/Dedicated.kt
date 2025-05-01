package uk.fishgames.fpsserver_outgame.dedicate_server

import uk.fishgames.fpsserver_outgame.matching.dto.GameSetupBoddari

class Dedicated(val id:String, val ip:String,var session:ArrayList<Session> = arrayListOf(), val serverUrl:String) {

    fun updateStatus(){
    }
}
enum class ServerStatus{
    Playing,
    Idle
}