package uk.fishgames.fpsserver_outgame.matching

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class MatchConfig {
    @Bean
    fun webClient(): WebClient = WebClient.builder().build()
}