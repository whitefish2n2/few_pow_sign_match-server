package uk.fishgames.fpsserver_outgame.dedicate_server

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.fishgames.fpsserver_outgame.dedicatedClients
import uk.fishgames.fpsserver_outgame.dedicate_server.dto.DedicateRegistResponseDto
import uk.fishgames.fpsserver_outgame.dedicate_server.dto.DedicatedDeleteDto
import uk.fishgames.fpsserver_outgame.dedicate_server.dto.DedicatedRegistDto

@RestController
@RequestMapping("/dedicated")
class DedicatedServerController(
    val dedicatedRegistService: DedicatedRegistService
) {


    @PostMapping("/creatededicated")
    fun CreateDedicate(@RequestBody dedicate: DedicatedRegistDto): DedicateRegistResponseDto {
        return dedicatedRegistService.registDedicated(dedicate)
    }

    @PostMapping("/deletededicated")
    fun deleteDedicate(@RequestBody dto: DedicatedDeleteDto): Any {
        try {
            val deleted = dedicatedClients.remove(dto.id)
            if(deleted == null)println("Someone Try to delete dedicated server, but there is no current dedicated server!")
        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity.badRequest().body("server delete failed")
        }
        println("one server deleted. ID: ${dto.id}")
        return ResponseEntity.ok("server deleted successfully.")
    }
}