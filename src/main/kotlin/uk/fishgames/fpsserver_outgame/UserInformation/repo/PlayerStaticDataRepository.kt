package uk.fishgames.fpsserver_outgame.UserInformation.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.fishgames.fpsserver_outgame.UserInformation.Entity.PlayerStaticDataEntity
import java.util.Optional

@Repository
interface PlayerStaticDataRepository : JpaRepository<PlayerStaticDataEntity, String> {

    fun findByUserId(userId: String): Optional<PlayerStaticDataEntity>

    fun existsByUserName(userName: String): Boolean

    fun findByUserName(nickname: String): Optional<PlayerStaticDataEntity>
}