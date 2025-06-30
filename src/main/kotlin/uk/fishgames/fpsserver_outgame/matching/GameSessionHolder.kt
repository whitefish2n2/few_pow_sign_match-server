package uk.fishgames.fpsserver_outgame.matching

import org.springframework.stereotype.Component
import uk.fishgames.fpsserver_outgame.dedicate_server.Session

@Component
class GameSessionHolder(){
    val runningSessions = HashMap<String, Session>()
    fun putSession(session: Session){
        runningSessions.set(session.gameId,session)
    }
    fun deleteSession(session: Session){
        runningSessions.remove(session.gameId)
    }
}