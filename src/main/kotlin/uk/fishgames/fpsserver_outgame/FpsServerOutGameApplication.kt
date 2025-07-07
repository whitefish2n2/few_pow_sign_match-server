package uk.fishgames.fpsserver_outgame

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import uk.fishgames.fpsserver_outgame.dedicate_server.Dedicated
import uk.fishgames.fpsserver_outgame.matching.GameSessionHolder

@SpringBootApplication
class FpsServerOutGameApplication
var dedicatedClients = HashMap<String, Dedicated>()

fun main(args: Array<String>) {
	runApplication<FpsServerOutGameApplication>(*args)
	val job = CoroutineScope(Dispatchers.Default).launch {
		while (isActive) {
			GameSessionHolder.tick()
			delay(1000)
		}
	}
}