package uk.fishgames.fpsserver_outgame.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthFilter(private val jwtProvider: uk.fishgames.fpsserver_outgame.security.JwtUtil) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val token = extractJwtFromHeader(request)

        if (token != null && jwtProvider.validateToken(token)) {
            val authentication = getAuthentication(token)
            SecurityContextHolder.getContext().authentication = authentication
        }

        chain.doFilter(request, response)
    }

    private fun extractJwtFromHeader(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization")
        return if (bearer != null && bearer.startsWith("Bearer ")) {
            bearer.substring(7)
        } else null
    }

    private fun getAuthentication(token: String): Authentication {
        val userId = jwtProvider.getUserIdFromToken(token)
        return UsernamePasswordAuthenticationToken(userId, null, emptyList())
    }
}

