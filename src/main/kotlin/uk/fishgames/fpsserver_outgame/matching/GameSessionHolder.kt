package uk.fishgames.fpsserver_outgame.matching

import org.springframework.stereotype.Component
import uk.fishgames.fpsserver_outgame.dedicate_server.Session
import java.util.concurrent.ConcurrentHashMap

@Component
object GameSessionHolder{
    val runningSessions = ConcurrentHashMap<String, Session>()
    fun putSession(session: Session){
        runningSessions.set(session.gameId,session)
    }
    fun deleteSession(session: Session){
        runningSessions.remove(session.gameId)
    }

    /**
     * todo:세션들 10초에 한번 순회하면서 상태 체크 등 하는 함수
     */
    fun tick(){
        for(i in runningSessions.values){
            i.tick()
        }
    }
}
