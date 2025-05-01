package uk.fishgames.fpsserver_outgame.matching

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.fishgames.fpsserver_outgame.FishUtil
import uk.fishgames.fpsserver_outgame.PlayerNotFoundException
import uk.fishgames.fpsserver_outgame.auth.repo.PlayerRepository
import uk.fishgames.fpsserver_outgame.dedicate_server.Dedicated
import uk.fishgames.fpsserver_outgame.dedicate_server.Session
import uk.fishgames.fpsserver_outgame.dedicatedClients
import uk.fishgames.fpsserver_outgame.matching.dto.GameSetupBoddari
import uk.fishgames.fpsserver_outgame.matching.dto.NewPlayerDto
import uk.fishgames.fpsserver_outgame.security.JwtUtil
import io.github.oshai.kotlinlogging.KotlinLogging

@Service
class MatchService(
    private val jwtUtil: JwtUtil,
    private val playerRepository: PlayerRepository,
    private val matchQueueManager: MatchQueueManager,
    private val webClient: WebClient,
    private val gameSessionStatusModule: GameSessionStatusModule
) {
    private val logger = KotlinLogging.logger {}
    fun getNewPlayerDto(id: String): NewPlayerDto {
        try {
            val p = playerRepository.findById(id)
            if(p.isEmpty) throw PlayerNotFoundException()
            val player = p.get()
            return NewPlayerDto(player.id,player.name)
        }
        catch(ex: Exception) {
            logger.info { "Error while trying to get player $id" }
            println(ex)
            throw PlayerNotFoundException()
        }
    }
    fun tryMakeMatch(mode: GameMode): Session? {
        val target = getDediServer()?:return null
        val players = matchQueueManager.makeMatch(mode) ?: return null
        val gameSetupBoddari = GameSetupBoddari("idMaster",players,mode,"nomalMap")
        try {
            logger.debug { "try make session like ${target.serverUrl}/makesession" }
            logger.debug { gameSetupBoddari.toString() }
            val body = Json.encodeToString(serializer(GameSetupBoddari::class.java), gameSetupBoddari)
            logger.debug { body }
            val res = webClient.post()
                .uri("${target.serverUrl}/makesession")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
            if(res != null) {
                val newSession = Session(
                    gameId = FishUtil.uuid(target.ip),
                    runningOn = target
                )
                gameSessionStatusModule.putSession(newSession)
                return newSession
            }
            else{
                return null
            }
        } catch (e: WebClientResponseException) {
            println("Match creation failed: ${e.statusCode} - ${e.responseBodyAsString}")
            return null
        }
    }
    var rrIndex:Int = 0
    fun getDediServer(): Dedicated? {
        try {
            logger.info { "try find dedi rrIndex: $rrIndex" }
            val v =dedicatedClients.entries.elementAt(
                rrIndex).value
            rrIndex = (rrIndex + 1) % dedicatedClients.count()
            return v
        }
        catch(ex:Exception) {
            logger.error { ex }
            return null
        }
    }
}
