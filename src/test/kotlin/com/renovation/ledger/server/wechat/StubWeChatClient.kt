package com.renovation.ledger.server.wechat

class StubWeChatClient(
    private val openidFor: (String) -> String = { "oid_$it" },
    private val unionid: String? = null,
) : WeChatClient {
    override fun code2Session(code: String, client: String): WeChatSession =
        WeChatSession(openid = openidFor(code), unionid = unionid)

    override fun phoneFromCode(phoneCode: String, client: String): String = when (phoneCode) {
        "phone_a" -> "13800001111"
        "phone_b" -> "13800002222"
        else -> "13800009999"
    }
}
