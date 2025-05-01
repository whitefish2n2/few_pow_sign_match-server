package uk.fishgames.fpsserver_outgame.matching

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import uk.fishgames.fpsserver_outgame.matching.dto.NewPlayerDto
import kotlin.collections.HashMap

@Component
class MatchQueueManager {
    //private val queue = HashMap<String,NewPlayerDto>()//string으로 할까요
    private val logger = KotlinLogging.logger {}
    private val queue = HashMap<GameMode, LinkedHashMap<String, NewPlayerDto>>()

    fun enqueue(gameMode: GameMode, playerId: String, newPlayerDto: NewPlayerDto) {
        queue.getOrPut(gameMode) { LinkedHashMap() }[playerId] = newPlayerDto;
        println("Enqueued: $playerId")
    }
    fun cancel(playerId: String) {
        for(q in queue) {
            q.value.remove(playerId)
        }
        println("Cancelled: $playerId")
    }
    var serverIdx = 0;

    fun makeMatch(gameMode: GameMode): List<NewPlayerDto>? {
        val count = gameMode.count
        if(queue[gameMode] == null||queue[gameMode]!!.count() < count)return null
        logger.info { "try make match, mode:$gameMode, count:$count" }
        val players = queue[gameMode]!!.values.take(count)
        return players
    }
}

