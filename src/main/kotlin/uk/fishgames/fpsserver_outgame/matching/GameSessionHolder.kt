package uk.fishgames.fpsserver_outgame.matching

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.fishgames.fpsserver_outgame.dedicate_server.Session
import java.util.concurrent.ConcurrentHashMap

@Component
class GameSessionHolder {
    val runningSessions = ConcurrentHashMap<String, Session>()

    fun putSession(session: Session) {
        runningSessions[session.gameId] = session
    }

    fun deleteSession(session: Session) {
        runningSessions.remove(session.gameId)
    }

    fun getSession(sessionId: String): Session? {
        return runningSessions[sessionId]
    }

    //10초에 한번 세션 돌면서 확인
    @Scheduled(fixedRate = 10000)
    fun tick() {
        for (session in runningSessions.values) {
            session.tick()
        }
    }
}
