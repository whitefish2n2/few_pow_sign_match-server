package uk.fishgames.fpsserver_outgame.auth

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.fishgames.fpsserver_outgame.*
import uk.fishgames.fpsserver_outgame.UserInformation.Entity.PlayerStaticDataEntity
import uk.fishgames.fpsserver_outgame.UserInformation.repo.PlayerStaticDataRepository
import uk.fishgames.fpsserver_outgame.auth.dto.RefreshDto
import uk.fishgames.fpsserver_outgame.auth.Entity.PlayerDataEntity
import uk.fishgames.fpsserver_outgame.auth.Entity.RefreshTokenEntity
import uk.fishgames.fpsserver_outgame.auth.dto.SignInDto
import uk.fishgames.fpsserver_outgame.auth.dto.SignInResponseDto
import uk.fishgames.fpsserver_outgame.auth.dto.SignInWithRefreshDto
import uk.fishgames.fpsserver_outgame.auth.dto.SignUpDto
import uk.fishgames.fpsserver_outgame.auth.repo.PlayerRepository
import uk.fishgames.fpsserver_outgame.auth.repo.RefreshTokenRepository
import uk.fishgames.fpsserver_outgame.security.JwtUtil
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Service
class AuthService(
    private val jwtUtil: JwtUtil,
    private val playerRepo: PlayerRepository,
    private val refreshTokenRepo: RefreshTokenRepository,
    private val playerStaticDataRepo: PlayerStaticDataRepository
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    fun validateToken(token: String): Boolean {
        val valid = jwtUtil.validateToken(token.replace("Bearer ", ""))
        if(!valid) throw InvalidJwtException()
        else return true;
    }
    fun signIn(info: SignInDto): SignInResponseDto {
        try {
            val player: PlayerDataEntity = playerRepo.findFirstById(info.id) ?: throw PlayerNotFoundException()

            if (player.password == FishUtil.hash(info.password)) {
                val token = createRefreshToken(player)
                val jwt = jwtUtil.createToken(player.id)
                return SignInResponseDto(jwt = jwt, refreshToken = token)
            } else throw LoginPasswordNotMatchException()
        } catch (e: Exception) {
            logger.error("error while signing in: ${info.id}", e)
            throw e
        }
    }

    fun signInWithRefreshToken(info: SignInWithRefreshDto): SignInResponseDto {
        try {
            if(!isValidRefreshToken(info.refreshToken))
                throw InvalidTokenException()
            val r = getIdFromRefreshToken(info.refreshToken)
            if(r != null) {
                val player: PlayerDataEntity = playerRepo.findFirstById(r) ?: throw PlayerNotFoundException()
                val token = createRefreshToken(player)
                val jwt = jwtUtil.createToken(player.id)
                return SignInResponseDto(jwt = jwt, refreshToken = token)
            } else throw PlayerNotFoundException()

        } catch (e: Exception) {
            logger.error("error while sign in with refresh token", e)
            throw e
        }
    }

    @Transactional(rollbackOn = [Exception::class])
    fun signUp(info: SignUpDto): PlayerDataEntity? {
        if (!isValidSignUpId(info.id)) throw AlreadyExistsIdException()

        //player data entity 생성
        val player = PlayerDataEntity()
        player.id = info.id
        player.password = FishUtil.hash(info.password)
        player.name = info.name

        //static info 생성
        val static = PlayerStaticDataEntity(
            userName = info.name,
            createdAt = LocalDateTime.now(),
        ).apply {
            user = player
        }
        player.staticData = static

        try {
            playerRepo.save(player)
            return player
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun refreshJwt(info: RefreshDto): String {
        if (!isValidRefreshToken(info.refreshToken)) throw InvalidTokenException()
        try {
            val id = jwtUtil.getUserIdFromToken(info.jwt)
            return jwtUtil.createToken(id)
        } catch (e: Exception) {
            logger.error("error while refresh jwt.", e)
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
            logger.error("error while create refresh token.", e)
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
    private fun getIdFromRefreshToken(token: String): String? {
        val t: RefreshTokenEntity? = refreshTokenRepo.findFirstByToken(token)
        return t?.id
    }
}