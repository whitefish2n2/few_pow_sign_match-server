package uk.fishgames.fpsserver_outgame.dedicate_server

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uk.fishgames.fpsserver_outgame.matching.GameMode
import uk.fishgames.fpsserver_outgame.matching.dto.DedicatedNewPlayerDto
import uk.fishgames.fpsserver_outgame.matching.dto.GameSetupBoddari
import uk.fishgames.fpsserver_outgame.matching.dto.MapEnum
import uk.fishgames.fpsserver_outgame.matching.dto.Player

class Dedicated(val id: String, val ip: String, var session: ArrayList<Session> = arrayListOf(), val serverUrl: String) {

    fun updateStatus() {

    }
}