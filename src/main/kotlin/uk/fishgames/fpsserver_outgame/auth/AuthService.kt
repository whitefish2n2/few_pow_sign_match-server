package uk.fishgames.fpsserver_outgame.auth

import org.springframework.stereotype.Service
import uk.fishgames.fpsserver_outgame.*
import uk.fishgames.fpsserver_outgame.auth.dto.RefreshDto
import uk.fishgames.fpsserver_outgame.auth.Entity.PlayerDataEntity
import uk.fishgames.fpsserver_outgame.auth.Entity.RefreshTokenEntity
import uk.fishgames.fpsserver_outgame.auth.dto.SignInDto
import uk.fishgames.fpsserver_outgame.auth.dto.SignInResponseDto
import uk.fishgames.fpsserver_outgame.auth.dto.SignUpDto
import uk.fishgames.fpsserver_outgame.auth.repo.PlayerRepository
import uk.fishgames.fpsserver_outgame.auth.repo.RefreshTokenRepository
import uk.fishgames.fpsserver_outgame.security.JwtUtil
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Service
class AuthService(
    private val jwtUtil: JwtUtil,
    private val playerRepo: PlayerRepository,
    private val refreshTokenRepo: RefreshTokenRepository
) {
    fun signIn(info: SignInDto): SignInResponseDto {
        try {
            val player: PlayerDataEntity = playerRepo.findFirstById(info.id) ?: throw PlayerNotFoundException()

            if (player.password == FishUtil.hash(info.password)) {
                val token = createRefreshToken(player)
                val jwt = jwtUtil.createToken(player.id)
                return SignInResponseDto(jwt = jwt, refreshToken = token)
            } else throw PlayerNotFoundException()
        } catch (e: Exception) {
            throw e
        }
    }

    fun signUp(info: SignUpDto): PlayerDataEntity? {
        if (!isValidSignUpId(info.id)) throw AlreadyExistsException()
        val newEntity = PlayerDataEntity()
        newEntity.id = info.id
        newEntity.password = FishUtil.hash(info.password)
        newEntity.name = info.name
        try {
            playerRepo.save(newEntity)
            return newEntity
        } catch (e: Exception) {
            throw e
        }
    }

    fun refreshJwt(info: RefreshDto): String {
        if (!isValidRefreshToken(info.refreshToken)) throw InvalidTokenException()
        try {
            val id = jwtUtil.getUserIdFromToken(info.jwt)
            return jwtUtil.createToken(id)
        } catch (e: Exception) {
            e.printStackTrace()
            throw InvalidJwtException()
        }
    }

    private fun createRefreshToken(info: PlayerDataEntity): String {
        try {
            val uuid = FishUtil.uuid(info.id)
            val newEntity = RefreshTokenEntity()
            newEntity.token = uuid
            newEntity.id = info.id
            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val expiry = now.plusMonths(2)
            newEntity.expireTime = expiry.toInstant()
            Instant.now(Clock.systemUTC())
            refreshTokenRepo.save(newEntity)
            return uuid
        } catch (e: Exception) {
            throw e
        }
    }

    private fun isValidSignUpId(id: String): Boolean {
        return !playerRepo.existsById(id)
    }

    private fun isValidRefreshToken(token: String): Boolean {
        val t: RefreshTokenEntity? = refreshTokenRepo.findFirstByToken(token)
        return !(t == null || t.expireTime!! < Instant.now())
    }
}