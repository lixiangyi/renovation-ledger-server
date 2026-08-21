package com.renovation.ledger.server.error

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ApiException::class)
    fun handle(ex: ApiException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(ex.status).body(
            mapOf("code" to ex.code, "message" to ex.message),
        )
}
