package uk.fishgames.fpsserver_outgame.UserInformation

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.fishgames.fpsserver_outgame.PlayerNotFoundException
import uk.fishgames.fpsserver_outgame.auth.dto.PlayerPrivateInformationDto
import uk.fishgames.fpsserver_outgame.auth.repo.PlayerRepository
import java.time.ZoneId

@Service
class PlayerInformationService(
    private val playerRepository: PlayerRepository
) {

    /**
     * @param userId : 요청을 보낸 유저의 id (JWT 등에서 추출)
     * @return PlayerPrivateInformationDto - 유저의 개인 정보를 담은 DTO
     * @throws PlayerNotFoundException 유저를 찾을 수 없거나 StaticData가 매핑되어 있지 않은 경우
     */
    @Transactional(readOnly = true)
    fun getCurrentPlayerInformation(userId: String): PlayerPrivateInformationDto {
        // 1. 유저 조회
        val player = playerRepository.findFirstById(userId)
            ?: throw PlayerNotFoundException()

        // 2. 연관된 StaticData 확인 (OneToOne 매핑)
        val staticData = player.staticData
            ?: throw PlayerNotFoundException() // 데이터 정합성 문제 발생 시 예외 처리

        // 3. LocalDateTime -> Long (Epoch Millis) 변환
        val createdAtMillis = staticData.createdAt
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // 4. DTO 조립 및 반환
        return PlayerPrivateInformationDto(
            userId = player.id,
            userName = player.name, // staticData.userName과 동일한 값으로 예상됨
            createdAt = createdAtMillis
        )
    }
}