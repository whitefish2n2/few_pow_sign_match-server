package uk.fishgames.fpsserver_outgame.matching

import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import uk.fishgames.fpsserver_outgame.security.JwtUtil
import java.lang.Exception

//웹소켓 auth 인터럽터(ws 요청에 spring security 대용으로 사용, 연결할때(핸드셰이킹 단계) 한번 확인. auth 인증 실패시 세션 close
@Component
class AuthHandshakeInterceptor(
    val jwtUtil: JwtUtil
) : HandshakeInterceptor {
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        println("Handshake started: $request")
        //user Id url 파라미터에서 빼오기
        try{
            val servletRequest = (request as ServletServerHttpRequest).servletRequest

            val token = servletRequest.getParameter("token") ?:
            servletRequest.getHeader("Authorization")?.removePrefix("Bearer ")?:return false

            if(!jwtUtil.validateToken(token)){
                return false
            }
            val userId = jwtUtil.getUserIdFromToken(token)
            attributes["userId"] = userId
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