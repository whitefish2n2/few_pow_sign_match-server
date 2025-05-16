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
import kotlinx.serialization.encodeToString
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import uk.fishgames.fpsserver_outgame.matching.dto.MatchFoundDto
import uk.fishgames.fpsserver_outgame.matching.dto.WsEventDto
import uk.fishgames.fpsserver_outgame.matching.dto.WsEventType

@Service
class MatchService(
    private val jwtUtil: JwtUtil,
    private val playerRepository: PlayerRepository,
    private val matchQueueManager: MatchQueueManager,
    private val webClient: WebClient,
    private val registry: MatchRegistry,
    private val gameSessionStatusModule: GameSessionStatusModule
) {
    private val logger = KotlinLogging.logger {}
    fun CreateNewPlayerDto(id: String): NewPlayerDto {
        try {
            val p = playerRepository.findById(id)
            if(p.isEmpty) throw PlayerNotFoundException()
            val player = p.get()
            return NewPlayerDto(player.id,player.name, FishUtil.uuid(player.name))
        }
        catch(ex: Exception) {
            logger.info { "Error while trying to get player $id" }
            println(ex)
            throw PlayerNotFoundException()
        }
    }
    fun registerPlayer(session: org.springframework.web.socket.WebSocketSession) {
        val userId:String = session.attributes["userId"].toString()

        val modeOrdinal: Int? = (session.attributes["gameMode"] as? String)?.toIntOrNull()

        val mode = GameMode.fromId(modeOrdinal)

        logger.info { "Registering new player $userId" }

        if(userId == "" || mode == null) return
        val dto = CreateNewPlayerDto(userId)

        registry.register(userId,session)
        matchQueueManager.enqueue(mode,userId,dto)

        tryMakeMatch(mode)
    }
    fun tryMakeMatch(mode: GameMode): Any? {
        val target = getDediServer()?:return null

        val players = matchQueueManager.makeMatch(mode) ?: return null

        val map = "nomalmap"

        val gameId = FishUtil.uuid(target.ip)

        val gameSetupBoddari = GameSetupBoddari(gameId,players,mode, map)

        logger.info { "try make session to ${target.serverUrl}/makesession" }
        logger.info { gameSetupBoddari.toString() }

        try {

            val body = Json.encodeToString(serializer(GameSetupBoddari::class.java), gameSetupBoddari)

            logger.debug { body }

            val res = webClient.post()
                .uri("${target.serverUrl}/makesession")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String::class.java)
                .doOnSuccess {
                    val newSession = Session(
                        gameId = gameSetupBoddari.gameId,
                        runningOn = target
                    )
                    val data = MatchFoundDto(newSession.gameId,newSession.runningOn.ip, map, players)
                    val dto = WsEventDto(WsEventType.MatchFound.toString(), Json.encodeToString(data))
                    val playerNotifyDto = Json.encodeToString(dto)
                    for(p in players){
                        if (registry.get(p.id)?.isOpen == true)
                        {
                            println("removing ${p.id}")
                            registry.get(p.id)?.sendMessage(TextMessage(playerNotifyDto))
                            registry.get(p.id)?.close(CloseStatus.NORMAL)
                            registry.remove(p.id)
                            matchQueueManager.remove(p.id)
                        }
                        else throw PlayerNotFoundException()
                    }

                    gameSessionStatusModule.putSession(newSession)
                }
                .doOnError {
                    logger.error(it) { "Error while making session" }
                    throw it
                }
                .subscribe()

        } catch (e: WebClientResponseException) {
            println("Match creation failed: ${e.statusCode} - ${e.responseBodyAsString}")
            return null
        }
        catch (e:Exception){
            logger.error(e) { "Error while making session" }
            return null
        }
        return true;
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
