package com.renovation.ledger.server.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class SmsLoginTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper

    @Test
    fun sendReturnsCodeThenLoginSameUserTwice() {
        val sendJson = mockMvc.post("/auth/sms/send") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"phone":"13800138000"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        val send = mapper.readValue<SmsSendResponse>(sendJson)
        assertNotNull(send.code)

        val login1 = login("13800138000", send.code!!)
        val send2Json = mockMvc.post("/auth/sms/send") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"phone":"13800138000"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        val send2 = mapper.readValue<SmsSendResponse>(send2Json)
        val login2 = login("13800138000", send2.code!!)
        assertEquals(login1.userId, login2.userId)
        assertEquals("13800138000", login2.phone)
    }

    @Test
    fun wrongCodeFails() {
        mockMvc.post("/auth/sms/send") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"phone":"13800138001"}"""
        }.andExpect { status { isOk() } }
        mockMvc.post("/auth/sms/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"phone":"13800138001","code":"000000"}"""
        }.andExpect { status { isBadRequest() } }
    }

    private fun login(phone: String, code: String): AuthResponse {
        val json = mockMvc.post("/auth/sms/login") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(SmsLoginRequest(phone = phone, code = code))
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readValue(json)
    }
}
