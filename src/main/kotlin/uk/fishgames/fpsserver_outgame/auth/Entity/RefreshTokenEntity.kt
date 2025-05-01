package uk.fishgames.fpsserver_outgame.auth.Entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = ("refresh_token"))
class RefreshTokenEntity {
    @Id
    @Column(name = ("token"), nullable = false,unique = true)
    var token:String?=null
    @Column(name=("id"), nullable = false)
    var id:String?=null
    @Column(name = ("expires_time"), nullable = false)
    var expireTime: Instant?=null
}