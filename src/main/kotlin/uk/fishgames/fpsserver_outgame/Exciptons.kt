package uk.fishgames.fpsserver_outgame

open class BaseException(
    val code: Int,
    message: String
) : RuntimeException(message)

class TestException(message: String = "오류 출력 테스트"):
    BaseException(ExceptionCode.TestException.code,message)
// 로그인 실패
class SignInFailedException(message: String = "아이디 또는 비밀번호가 일치하지 않습니다.") :
    BaseException(ExceptionCode.SignInFailedException.code, message)

// 이미 있는 아이디
class AlreadyExistsIdException(message: String = "이미 존재하는 아이디입니다.") :
    BaseException(ExceptionCode.AlreadyExistsIdException.code, message)
class PlayerNotFoundException(message: String = "플레이어를 찾을 수 없습니다.") :
    BaseException(ExceptionCode.PlayerNotFoundException.code, message)
class LoginFailedException(message: String = "로그인에 실패하였습니다.") :
    BaseException(ExceptionCode.LoginFailedException.code, message)
class LoginNotRegisteredIdException(message: String = "존재하지 않는 아이디입니다.") :
    BaseException(ExceptionCode.LoginNotRegisteredIdException.code, message)
class LoginPasswordNotMatchException(message: String = "비밀번호가 다릅니다.") :
        BaseException(ExceptionCode.LoginPasswordNotMatchedException.code, message)

class InvalidMatchException(message: String = "유효하지 않은 매치 id입니다."):
    BaseException(ExceptionCode.InvalidMatchException.code, message)
// 플레이어 찾을 수 없음

class InvalidTokenException(message : String = "유효하지 않은 토큰입니다.") :
    BaseException(ExceptionCode.InvalidTokenException.code, message)
class InvalidJwtException(message: String = "유효하지 않은 jwt입니다.") :
    BaseException(ExceptionCode.InvalidJwtException.code, message)
class DediSecretKeyNotMatchedException(message:String = "데디케이티드 서버 등록 키가 맞지 않습니다."):
    BaseException(ExceptionCode.DediSecretKeyNotMatchedException.code, message)
class NotDefinedError(message: String= "정의되지 않은 에러입니다. whitefish822@gmail.com으로 연락해주세요."):
    BaseException(ExceptionCode.NotDefinedError.code,message)

enum class ExceptionCode(val code: Int) {
    TestException(1),
    //auth error
    SignInFailedException(4001),
    AlreadyExistsIdException(4002),
    PlayerNotFoundException(4003),
    LoginFailedException(4004),
    LoginNotRegisteredIdException(4005),
    LoginPasswordNotMatchedException(4006),
    //match error
    InvalidMatchException(4010),
    //DedicatedServer error
    DediSecretKeyNotMatchedException(4060),
    //token error
    InvalidTokenException(4070),
    InvalidJwtException(4071),


    //not defined
    NotDefinedError(4999),
}
/*
400-|auth error
410-|match error
406-|dedicated server error
407-|token error(do token regenerate)
*/