package de.surala.example.eco.order.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest


@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException, request: WebRequest
    ): ResponseEntity<Any> {
        val errorResponse = mapOf(
            "statusCode" to "BAD_REQUEST",
            "typeMessageCode" to "problemDetail.type.${ex.javaClass.canonicalName}",
            "body" to mapOf(
                "title" to "Bad Request",
                "status" to HttpStatus.BAD_REQUEST.value(),
                "detail" to ex.message
            )
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    // Catch-all for other exceptions
    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        ex: Exception, request: WebRequest
    ): ResponseEntity<Any> {
        val errorResponse: ErrorResponse =
            ErrorResponse
                .builder(ex, HttpStatus.INTERNAL_SERVER_ERROR,request.getDescription(false))
                .build()

        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
