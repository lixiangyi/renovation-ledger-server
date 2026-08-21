package com.renovation.ledger.server.auth

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MeController(
    private val authService: AuthService,
) {
    @GetMapping("/me")
    fun getMe(): MeResponse = authService.getMe()

    @PatchMapping("/me")
    fun updateMe(@RequestBody request: UpdateMeRequest): MeResponse =
        authService.updateMe(request)
}
