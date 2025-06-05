package uk.fishgames.fpsserver_outgame

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MainController(
) {
    @GetMapping("/hi")
    fun getText(): String {
        return "hello from fish"
    }
    @GetMapping("/givemeerror")
    fun giveError(): Any {
        throw TestException();
    }
}