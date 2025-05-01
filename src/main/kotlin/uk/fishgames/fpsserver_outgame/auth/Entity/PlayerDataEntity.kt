package uk.fishgames.fpsserver_outgame.auth.Entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = ("playerdata"))
class PlayerDataEntity {
    @Id
    @Column(nullable = false,unique = true)
    var id: String = ""
    @Column(nullable = false,unique = true)
    var name: String = ""
    @Column(nullable = false)
    var password: String = ""
}