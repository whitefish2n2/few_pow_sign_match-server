package uk.fishgames.fpsserver_outgame.auth.repo

import org.springframework.data.jpa.repository.JpaRepository
import uk.fishgames.fpsserver_outgame.auth.Entity.RefreshTokenEntity

interface RefreshTokenRepository: JpaRepository<RefreshTokenEntity, String> {
    fun findFirstByToken(token: String): RefreshTokenEntity?
}