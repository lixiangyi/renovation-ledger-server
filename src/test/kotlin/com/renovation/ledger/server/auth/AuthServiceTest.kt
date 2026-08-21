package com.renovation.ledger.server.auth

import com.renovation.ledger.server.wechat.StubWeChatClient
import com.renovation.ledger.server.wechat.WeChatClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@SpringBootTest
class AuthServiceTest {
    @TestConfiguration
    class WeChatStubConfig {
        @Bean
        @Primary
        fun weChatClient(): WeChatClient = StubWeChatClient(
            openidFor = { "mp_openid_1" },
            unionid = "union_1",
        )
    }

    @Autowired lateinit var authService: AuthService

    @Test
    fun wechatLoginTwiceSameOpenidSameUser() {
        val first = authService.loginWeChat(WeChatLoginRequest(code = "c1", client = "mp"))
        val second = authService.loginWeChat(WeChatLoginRequest(code = "c2", client = "mp"))
        assertEquals(first.userId, second.userId)
        assertNotNull(first.token)
    }
}
