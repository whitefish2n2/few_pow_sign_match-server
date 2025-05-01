package uk.fishgames.fpsserver_outgame

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import uk.fishgames.fpsserver_outgame.dedicate_server.Dedicated

@SpringBootApplication
class FpsServerOutGameApplication
var dedicatedClients = HashMap<String, Dedicated>()

fun main(args: Array<String>) {
	runApplication<FpsServerOutGameApplication>(*args)
}