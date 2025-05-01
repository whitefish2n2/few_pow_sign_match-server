package uk.fishgames.fpsserver_outgame.auth.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.fishgames.fpsserver_outgame.auth.Entity.PlayerDataEntity

@Repository
interface PlayerRepository : JpaRepository<PlayerDataEntity, String> {
    fun findByName(name: String): PlayerDataEntity?

    fun findFirstByIdAndPassword(id: String, password: String): PlayerDataEntity?

    fun save(entity: PlayerDataEntity): PlayerDataEntity?

    fun findFirstById(id: String): PlayerDataEntity?
}