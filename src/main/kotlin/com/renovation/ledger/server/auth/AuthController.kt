package com.renovation.ledger.server.auth

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/auth/wechat")
    fun wechat(@RequestBody request: WeChatLoginRequest): AuthResponse =
        authService.loginWeChat(request)

    @PostMapping("/auth/bind-phone")
    fun bindPhone(@RequestBody request: BindPhoneRequest): AuthResponse =
        authService.bindPhone(request)

    @PostMapping("/auth/dev-login")
    fun devLogin(@RequestBody body: Map<String, String>): AuthResponse =
        authService.devLogin(body["label"] ?: "dev")

    @PostMapping("/auth/sms/send")
    fun smsSend(@RequestBody request: SmsSendRequest): SmsSendResponse =
        authService.sendSms(request)

    @PostMapping("/auth/sms/login")
    fun smsLogin(@RequestBody request: SmsLoginRequest): AuthResponse =
        authService.loginSms(request)
}
