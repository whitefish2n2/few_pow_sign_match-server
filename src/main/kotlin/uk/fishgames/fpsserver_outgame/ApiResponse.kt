package uk.fishgames.fpsserver_outgame

class ApiResponse<T> (
    var code: Int = ApiResponseCode.Default.code,
    var msg: String = "",
    var data: T? = null
)
enum class ApiResponseCode(val code: Int) {
    Default(2000),
    SignInSuccess(2101),
    SignUpSuccess(2102),
    JwtRefreshed(2103),
    RefreshTokenRegenerate(2104),
    IsValidJwt(2105),
    IsUnValidJwt(2106),
}
//21xx:auth code