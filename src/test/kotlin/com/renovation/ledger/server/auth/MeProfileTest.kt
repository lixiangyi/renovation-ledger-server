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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class MeProfileTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper

    @Test
    fun getAndPatchNickname() {
        val sendJson = mockMvc.post("/auth/sms/send") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"phone":"13800138999"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        val send = mapper.readValue<SmsSendResponse>(sendJson)
        assertNotNull(send.code)

        val loginJson = mockMvc.post("/auth/sms/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"phone":"13800138999","code":"${send.code}"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        val login = mapper.readValue<AuthResponse>(loginJson)
        val token = login.token

        val meBefore: MeResponse = mapper.readValue(
            mockMvc.get("/me") {
                header("Authorization", "Bearer $token")
            }.andExpect { status { isOk() } }.andReturn().response.contentAsString,
        )
        assertEquals(login.userId, meBefore.userId)
        assertEquals("8999", meBefore.nickname)

        val patched: MeResponse = mapper.readValue(
            mockMvc.patch("/me") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = """{"nickname":"小明"}"""
            }.andExpect { status { isOk() } }.andReturn().response.contentAsString,
        )
        assertEquals("小明", patched.nickname)

        val meAfter: MeResponse = mapper.readValue(
            mockMvc.get("/me") {
                header("Authorization", "Bearer $token")
            }.andExpect { status { isOk() } }.andReturn().response.contentAsString,
        )
        assertEquals("小明", meAfter.nickname)

        val blank: MeResponse = mapper.readValue(
            mockMvc.patch("/me") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = """{"nickname":"   "}"""
            }.andExpect { status { isOk() } }.andReturn().response.contentAsString,
        )
        assertEquals("我", blank.nickname)
    }

    @Test
    fun unauthenticatedMeReturns401() {
        mockMvc.get("/me").andExpect { status { isUnauthorized() } }
        mockMvc.patch("/me") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"nickname":"x"}"""
        }.andExpect { status { isUnauthorized() } }
    }
}
