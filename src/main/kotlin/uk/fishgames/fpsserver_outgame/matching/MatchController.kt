package uk.fishgames.fpsserver_outgame.matching

import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/match")
class MatchController(
    private val matchWebSocketHandler: MatchWebSocketHandler,
    private val matchQueueManager: MatchQueueManager,
    private val matchService: MatchService,
) {
    var spreadIndex: Int = 0
    @GetMapping("/matching")
    fun matching(authentication: Authentication, gameMode: GameMode): Any {
        val player = matchService.getNewPlayerDto(authentication.name)
        matchQueueManager.enqueue(gameMode,authentication.name,player)
        return ResponseEntity.ok("")
        /*
        if (dedicatedClients.isEmpty()) {
            return "there is no connectable server"
        }
        dedicatedClients.entries.toList()[spreadIndex % dedicatedClients.size].value.let { dedi: Dedicated ->
            for (v in dedi.Servers) {
                println(v.status)
                if (v.status == ServerStatus.Idle) {
                    return dedi.ip + ":" + v.port
                }
            }
        }
        spreadIndex++
        return HttpStatus.NOT_FOUND;
    }
    */
    }
}