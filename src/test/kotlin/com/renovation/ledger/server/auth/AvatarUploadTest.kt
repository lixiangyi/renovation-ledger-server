package com.renovation.ledger.server.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class AvatarUploadTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper

    private fun loginToken(phone: String = "13800138888"): String {
        val sendJson = mockMvc.post("/auth/sms/send") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"phone":"$phone"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        val send = mapper.readValue<SmsSendResponse>(sendJson)
        assertNotNull(send.code)
        val loginJson = mockMvc.post("/auth/sms/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"phone":"$phone","code":"${send.code}"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readValue<AuthResponse>(loginJson).token
    }

    @Test
    fun uploadThenGetMeAndPublicFile() {
        val token = loginToken()
        val jpeg = MockMultipartFile(
            "file",
            "avatar.jpg",
            "image/jpeg",
            byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()),
        )
        val uploaded: MeResponse = mapper.readValue(
            mockMvc.multipart("/me/avatar") {
                header("Authorization", "Bearer $token")
                file(jpeg)
            }.andExpect { status { isOk() } }.andReturn().response.contentAsString,
        )
        assertNotNull(uploaded.avatarUrl)
        assertTrue(uploaded.avatarUrl!!.startsWith("/avatars/"))

        val me: MeResponse = mapper.readValue(
            mockMvc.get("/me") {
                header("Authorization", "Bearer $token")
            }.andExpect { status { isOk() } }.andReturn().response.contentAsString,
        )
        assertEquals(uploaded.avatarUrl, me.avatarUrl)

        mockMvc.get(uploaded.avatarUrl!!).andExpect { status { isOk() } }

        val cleared: MeResponse = mapper.readValue(
            mockMvc.delete("/me/avatar") {
                header("Authorization", "Bearer $token")
            }.andExpect { status { isOk() } }.andReturn().response.contentAsString,
        )
        assertNull(cleared.avatarUrl)

        mockMvc.get(uploaded.avatarUrl!!).andExpect { status { isNotFound() } }
    }

    @Test
    fun rejectNonImageAndUnauthenticated() {
        val token = loginToken("13800138887")
        val txt = MockMultipartFile(
            "file",
            "a.txt",
            "text/plain",
            "hello".toByteArray(),
        )
        mockMvc.multipart("/me/avatar") {
            header("Authorization", "Bearer $token")
            file(txt)
        }.andExpect { status { isBadRequest() } }

        mockMvc.multipart("/me/avatar") {
            file(txt)
        }.andExpect { status { isUnauthorized() } }

        mockMvc.delete("/me/avatar").andExpect { status { isUnauthorized() } }
    }
}
