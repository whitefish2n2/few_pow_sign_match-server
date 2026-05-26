import org.springframework.web.socket.CloseStatus


object CustomWebsocketCloseCode {
    /**
     * 4001: 다른 기기에서 접속하여 기존 연결을 강제 종료함 (중복 로그인)
     */
    val DUPLICATE_LOGIN = CloseStatus(4001, "Logged in from another device")

    /**
     * 4002: 캐릭터 픽 완료 및 게임 세션 시작으로 인한 정상 종료
     */
    val GAME_STARTED = CloseStatus(4002, "Game Started")

    val KICKED_BY_ADMIN = CloseStatus(4003, "관리자에 의해 추방되었습니다.")
}