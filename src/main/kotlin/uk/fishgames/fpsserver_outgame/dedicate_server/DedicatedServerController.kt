package uk.fishgames.fpsserver_outgame.dedicate_server

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.fishgames.fpsserver_outgame.FishUtil
import uk.fishgames.fpsserver_outgame.dedicatedClients
import uk.fishgames.fpsserver_outgame.dedicate_server.dto.DedicateRegistResponseDto
import uk.fishgames.fpsserver_outgame.dedicate_server.dto.DedicatedRegistDto

@RestController
@RequestMapping("/dedicated")
class DedicatedServerController(
    val dedicatedRegistService: DedicatedRegistService
) {


    @PostMapping("/creatededicated")
    fun CreateDedicate(@RequestBody dedicate: DedicatedRegistDto): DedicateRegistResponseDto {
        return dedicatedRegistService.createDedicated(dedicate)
    }

    @PostMapping("/deletededicated")
    fun deleteDedicate(@RequestBody id: String): Any {
        try {
            dedicatedClients.remove(id)
        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity.badRequest().body("server delete failed")
        }
        println("one server deleted")
        return ResponseEntity.ok("server deleted")
    }
}