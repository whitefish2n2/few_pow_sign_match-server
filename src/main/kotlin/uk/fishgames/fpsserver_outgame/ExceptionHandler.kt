package uk.fishgames.fpsserver_outgame

import jakarta.persistence.NoResultException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

data class ErrorResponse(val code: Int, val message: String)
@ControllerAdvice
class ExceptionHandler {
    @ExceptionHandler(BaseException::class)
    fun handleBaseException(e: BaseException): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(e.code, e.message ?: "알 수 없는 오류")
        return ResponseEntity.badRequest().body(body)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnhandled(e: Exception): ResponseEntity<ErrorResponse> {
        e.printStackTrace()
        val body = ErrorResponse(5000, "서버 내부 오류입니다.")
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body)
    }
}