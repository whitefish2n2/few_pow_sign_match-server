package uk.fishgames.fpsserver_outgame.matching

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import uk.fishgames.fpsserver_outgame.matching.dto.Player
import uk.fishgames.fpsserver_outgame.matching.ws.MatchWebsocketRegistry
import kotlin.collections.HashMap

@Component
class MatchQueueManager(private val matchWebsocketRegistry: MatchWebsocketRegistry) {
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

        //플레이어 큐에서 꺼내오기
        val count = gameMode.count//게임 사람 수
        if(queue[gameMode] == null||queue[gameMode]!!.count() < count)return null
        logger.info { "try make match, mode:$gameMode, count:$count" }
        val players = queue[gameMode]!!.values.take(count)
        for(player in players) {
            logger.info { "Player ${player.id}" }
            if(!(matchWebsocketRegistry.get(player.key)?.isOpen ?:false)){
                logger.info{"Player ${player.id} websocket is not open. so cancel the match"}
                cancel(player.key)
                return null;
            }
        }

        //플레이어 팀 설정하기
        players.forEachIndexed { index, player ->
            // 1, 2, 1, 2 순서로 배정
            player.team = (index % 2) + 1
        }
        return players
    }
}

