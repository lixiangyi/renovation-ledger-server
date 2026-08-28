package com.renovation.ledger.server.auth

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.junit.jupiter.api.Test

@SpringBootTest(properties = ["app.sms.return-code-in-response=false"])
@AutoConfigureMockMvc
class SmsSendDisabledTest {
    @Autowired lateinit var mockMvc: MockMvc

    @Test
    fun sendReturns501WhenReturnCodeDisabled() {
        mockMvc.post("/auth/sms/send") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"phone":"13800138000"}"""
        }.andExpect {
            status { isEqualTo(501) }
            jsonPath("$.message") { value("正式环境短信未开通") }
        }
    }
}
