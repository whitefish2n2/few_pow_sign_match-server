package uk.fishgames.fpsserver_outgame.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Value
import uk.fishgames.fpsserver_outgame.InvalidJwtException
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtUtil {
    private val logger: org.slf4j.Logger? = LoggerFactory.getLogger(JwtUtil::class.java)

    @Value("\${jwt.secret}")
    lateinit var secret: String
    private lateinit var key: SecretKey
    private val expirationMillis = 60// 60분

    @PostConstruct
    fun init() {
        key = Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun createToken(userId: String): String {
        logger?.info(secret)
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val expiry = now.plusMinutes(expirationMillis.toLong())

        val nowDate = Date.from(now.toInstant())
        val expiryDate = Date.from(expiry.toInstant())

        return Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(nowDate)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            val claims = getClaims(token)
            if (claims.expiration.before(Date())) {
                println("Token validation failed: Token expired.")
                false
            } else {
                println("Token validation successful: Token is valid.")
                true
            }
        } catch (e: Exception) {
            println("Token validation failed: ${e.message}")
            false
        }
    }

    fun getUserIdFromToken(token: String): String {
        try {
            return getClaims(token).subject
        } catch (e: ExpiredJwtException) {
            return e.claims.subject
        } catch (e: Exception) {
            throw InvalidJwtException()
        }
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }
}
