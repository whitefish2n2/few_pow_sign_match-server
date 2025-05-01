package uk.fishgames.fpsserver_outgame.dedicate_server.dto

import org.springframework.http.HttpStatus

data class DedicateRegistResponseDto(val ServerId:String = "Default", val httpStatusCode: Int = HttpStatus.BAD_REQUEST.value())