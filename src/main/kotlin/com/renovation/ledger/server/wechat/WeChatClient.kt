package com.renovation.ledger.server.wechat

data class WeChatSession(val openid: String, val unionid: String?)

interface WeChatClient {
    fun code2Session(code: String, client: String): WeChatSession
    fun phoneFromCode(phoneCode: String, client: String): String
}
