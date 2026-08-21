package com.renovation.ledger.server.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.renovation.ledger.server.wechat.StubWeChatClient
import com.renovation.ledger.server.wechat.WeChatClient
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class BindPhoneTest {
    @TestConfiguration
    class WeChatStubConfig {
        @Bean
        @Primary
        fun weChatClient(): WeChatClient = StubWeChatClient()
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper

    @Test
    fun bindSuccessThenSecondUserConflict() {
        val a = login("phone_a")
        mockMvc.post("/auth/bind-phone") {
            header("Authorization", "Bearer $a")
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(BindPhoneRequest(phoneCode = "phone_a", client = "mp"))
        }.andExpect { status { isOk() } }
        val b = login("phone_b")
        mockMvc.post("/auth/bind-phone") {
            header("Authorization", "Bearer $b")
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(BindPhoneRequest(phoneCode = "phone_a", client = "mp"))
        }.andExpect { status { isConflict() } }
    }

    private fun login(code: String): String {
        val json = mockMvc.post("/auth/wechat") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(WeChatLoginRequest(code = code, client = "mp"))
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readValue<AuthResponse>(json).token
    }
}
