package uk.fishgames.fpsserver_outgame.matching

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import uk.fishgames.fpsserver_outgame.matching.dto.Player
import kotlin.collections.HashMap

@Component
class MatchQueueManager {
    //private val queue = HashMap<String,NewPlayerDto>()//string으로 할까요
    private val logger = KotlinLogging.logger {}
    private val queue = HashMap<GameMode, LinkedHashMap<String, Player>>()

    fun enqueue(gameMode: GameMode, userKey: String, Player: Player) {
        queue.getOrPut(gameMode) { LinkedHashMap() }[userKey] = Player;
        println("Enqueued: $userKey")
    }
    fun cancel(playerId: String) {
        for(q in queue) {
            q.value.remove(playerId)
        }
        logger.info { ("Cancelled: $playerId") }
    }
    var serverIdx = 0;

    fun makeMatch(gameMode: GameMode): List<Player>? {
        val count = gameMode.count
        if(queue[gameMode] == null||queue[gameMode]!!.count() < count)return null
        logger.info { "try make match, mode:$gameMode, count:$count" }
        val players = queue[gameMode]!!.values.take(count)
        for(player in players) {
            if(!(player.matchWebsocket?.isOpen ?:false)){
                return null;
            }
        }
        return players
    }
}

