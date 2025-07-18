package uk.fishgames.fpsserver_outgame.UserInformation.Entity

import jakarta.persistence.*
import uk.fishgames.fpsserver_outgame.auth.Entity.PlayerDataEntity
import java.time.LocalDateTime

@Entity
@Table(name = "player_static_data")
class PlayerStaticDataEntity(
    @Column(name = "user_name", length = 256, nullable = false, unique = true)
    var userName: String = "",

    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @Column(name = "user_id", length = 256)
    var userId: String? = null;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(
        name = "user_id",
        foreignKey = ForeignKey(name = "player_id_fk")
    )
    lateinit var user: PlayerDataEntity
}