package uk.fishgames.fpsserver_outgame.matching

import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.server.HandshakeInterceptor
import uk.fishgames.fpsserver_outgame.InvalidJwtException
import uk.fishgames.fpsserver_outgame.security.JwtUtil
import java.lang.Exception

@Component
class AuthHandshakeInterceptor(
    val jwtUtil: JwtUtil,
    val matchQueueManager: MatchQueueManager
) : HandshakeInterceptor {
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        println("Handshake started: $request")
        try{
            val servletRequest = (request as ServletServerHttpRequest).servletRequest
            val token = servletRequest.getParameter("token") ?: return false
            if(!jwtUtil.validateToken(token))return false
            val userId = jwtUtil.getUserIdFromToken(token)
            attributes["userId"] = userId
            attributes["gameMode"] = servletRequest.getParameter("gameMode")
            return true
        }catch(e: Exception){
            return false
        }
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
    }

}