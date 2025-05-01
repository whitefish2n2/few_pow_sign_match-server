package uk.fishgames.fpsserver_outgame.matching

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val matchWebSocketHandler: MatchWebSocketHandler,
    private val authHandshakeInterceptor: AuthHandshakeInterceptor,
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(matchWebSocketHandler, "/match-wait")
            .addInterceptors(authHandshakeInterceptor)
            .setAllowedOrigins("*")
        println("Auth HandshakeInterceptor: $authHandshakeInterceptor")
    }
}
