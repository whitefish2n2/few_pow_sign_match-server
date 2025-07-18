package uk.fishgames.fpsserver_outgame.auth.Entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.fishgames.fpsserver_outgame.UserInformation.Entity.PlayerStaticDataEntity

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

    //user static data 테이블과 관계 설정
    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, optional = false)
    var staticData: PlayerStaticDataEntity? = null
}