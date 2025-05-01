package uk.fishgames.fpsserver_outgame.dedicate_server

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.fishgames.fpsserver_outgame.FishUtil
import uk.fishgames.fpsserver_outgame.dediSecretKeyNotMatchedException
import uk.fishgames.fpsserver_outgame.dedicate_server.dto.DedicateRegistResponseDto
import uk.fishgames.fpsserver_outgame.dedicate_server.dto.DedicatedRegistDto
import uk.fishgames.fpsserver_outgame.dedicatedClients

@Service
class DedicatedRegistService {
    @Value("\${dedicate.secret-key}")
    var holymolySafeKey: String = "default"
    fun createDedicated(dedicate: DedicatedRegistDto):DedicateRegistResponseDto {
        val serverId = FishUtil.hash(dedicate.ip)
        try {
            if (dedicate.key != holymolySafeKey) {
                throw dediSecretKeyNotMatchedException()
            }
            if (dedicatedClients.get(serverId) == null) {
                dedicatedClients.put(serverId, Dedicated(serverId, dedicate.ip, dedicate.sessions, dedicate.url))
                println("new dedicate Server is on the rail port: ${dedicate.ip} ip: ${dedicate.ip}")
            } else {
                dedicatedClients.remove(serverId)
                dedicatedClients.put(
                    FishUtil.hash(serverId
                ), Dedicated(serverId, dedicate.ip, dedicate.sessions, dedicate.url))
                println("Server replaced. port: ${dedicate.ip} ip: ${dedicate.ip}")

            }
        } catch (e: Exception) {
            e.printStackTrace()
            return DedicateRegistResponseDto(httpStatusCode = HttpStatus.BAD_REQUEST.value());
        }
        return DedicateRegistResponseDto(serverId, HttpStatus.OK.value());
    }
}