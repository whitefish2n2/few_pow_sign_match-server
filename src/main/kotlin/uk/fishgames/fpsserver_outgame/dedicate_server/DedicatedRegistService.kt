package uk.fishgames.fpsserver_outgame.dedicate_server

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.fishgames.fpsserver_outgame.AlreadyExistDedicatedServerException
import uk.fishgames.fpsserver_outgame.FishUtil
import uk.fishgames.fpsserver_outgame.DediSecretKeyNotMatchedException
import uk.fishgames.fpsserver_outgame.dedicate_server.dto.DedicateRegistResponseDto
import uk.fishgames.fpsserver_outgame.dedicate_server.dto.DedicatedRegistDto
import uk.fishgames.fpsserver_outgame.dedicatedClients

@Service
class DedicatedRegistService {
    @Value("\${dedicate.secret-key}")
    var holymolySafeKey: String = "default"
    fun registDedicated(dedicate: DedicatedRegistDto):DedicateRegistResponseDto {
        val serverId = FishUtil.hash(dedicate.ip)
        try {
            if (dedicate.key != holymolySafeKey) {
                throw DediSecretKeyNotMatchedException()
            }
            if (dedicatedClients.get(serverId) == null) {
                dedicatedClients.put(serverId, Dedicated(serverId, dedicate.ip, dedicate.sessions, dedicate.url))
                println("new dedicate Server is on the rail id: ${serverId} ip: ${dedicate.ip}")
            } else {
                println("Invalid Dedicated Server Create Request Detected.")
                throw AlreadyExistDedicatedServerException();
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return DedicateRegistResponseDto(httpStatusCode = HttpStatus.BAD_REQUEST.value());
        }
        return DedicateRegistResponseDto(serverId, HttpStatus.OK.value());
    }
    fun deleteDedicated(id:String) {
        dedicatedClients.remove(id)
    }
}