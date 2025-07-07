package uk.fishgames.fpsserver_outgame.matching

import org.springframework.stereotype.Component
import uk.fishgames.fpsserver_outgame.dedicate_server.Session

@Component
object GameSessionHolder{
    val runningSessions = HashMap<String, Session>()
    fun putSession(session: Session){
        runningSessions.set(session.gameId,session)
    }
    fun deleteSession(session: Session){
        runningSessions.remove(session.gameId)
    }

    /**
     * 세션들 1초에 한번 순회하면서 상태 체크 등 하는 함수
     */
    fun tick(){
        for(i in runningSessions.values){
            i.tick()
        }
    }
}
