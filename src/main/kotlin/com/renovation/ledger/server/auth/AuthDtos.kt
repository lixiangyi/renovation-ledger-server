package com.renovation.ledger.server.auth

data class WeChatLoginRequest(
    val code: String,
    val client: String,
)

data class AuthResponse(
    val userId: String,
    val token: String,
    val nickname: String,
    val phone: String? = null,
)

data class BindPhoneRequest(
    val phoneCode: String,
    val client: String = "mp",
)

data class SmsSendRequest(
    val phone: String,
)

data class SmsSendResponse(
    val expiresInSec: Long,
    val code: String? = null,
)

data class SmsLoginRequest(
    val phone: String,
    val code: String,
)

data class MeResponse(
    val userId: String,
    val nickname: String,
    val phone: String? = null,
)

data class UpdateMeRequest(
    val nickname: String,
)

