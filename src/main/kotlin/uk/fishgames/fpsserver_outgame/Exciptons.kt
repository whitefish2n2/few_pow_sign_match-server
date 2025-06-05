package uk.fishgames.fpsserver_outgame

open class BaseException(
    val code: Int,
    message: String
) : RuntimeException(message)

class TestException(message: String = "오류 출력 테스트"):
    BaseException(1,message)
// 로그인 실패
class SignInFailedException(message: String = "아이디 또는 비밀번호가 일치하지 않습니다.") :
    BaseException(4001, message)

// 이미 있는 아이디
class AlreadyExistsException(message: String = "이미 존재하는 아이디입니다.") :
    BaseException(4002, message)

class invalidMatchException(message: String = "유효하지 않은 매치 id입니다."):
    BaseException(4010, message)
class invalidMatch
// 플레이어 찾을 수 없음
class PlayerNotFoundException(message: String = "플레이어를 찾을 수 없습니다.") :
    BaseException(4003, message)
class InvalidTokenException(message : String = "유효하지 않은 토큰입니다.") :
    BaseException(4070, message)
class InvalidJwtException(message: String = "유효하지 않은 jwt입니다.") :
    BaseException(4071, message)
class dediSecretKeyNotMatchedException(message:String = "데디케이티드 서버 등록 키가 맞지 않습니다."):
    BaseException(4060, message)
class NotDefinedError(message: String= "정의되지 않은 에러입니다. whitefish822@gmail.com으로 연락해주세요."):
    BaseException(4999,message)
/*
400-|auth error
410-|match error
406-|dedicated server error
407-|token error(do token regenerate)
*/