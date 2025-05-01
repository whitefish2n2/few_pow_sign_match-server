package uk.fishgames.fpsserver_outgame.dedicate_server.dto

import uk.fishgames.fpsserver_outgame.dedicate_server.Session

class DedicatedRegistDto {
    var ip:String = "0.0.0.0"
    var key:String="null"
    var url:String="null"
    var sessions:ArrayList<Session> = ArrayList()
}