package com.renovation.ledger.server.ledger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.renovation.ledger.server.auth.AuthResponse
import com.renovation.ledger.server.auth.WeChatLoginRequest
import com.renovation.ledger.server.wechat.StubWeChatClient
import com.renovation.ledger.server.wechat.WeChatClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class LedgerCreateTest {
    @TestConfiguration
    class WeChatStubConfig {
        @Bean
        @Primary
        fun weChatClient(): WeChatClient = StubWeChatClient()
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun createEmptyLedgerThenGet() {
        val token = login("create1")
        val json = mockMvc.post("/ledgers") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(
                CreateLedgerRequest(name = "新云账本", localId = "local_create1"),
            )
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        val created = mapper.readValue<LedgerSnapshot>(json)
        assertEquals("新云账本", created.name)
        assertEquals(0, created.items.size)
        val listJson = mockMvc.get("/ledgers") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        val list: List<LedgerSummaryDto> = mapper.readValue(listJson)
        assertEquals(1, list.size)
        assertEquals(created.id, list[0].id)
        org.junit.jupiter.api.Assertions.assertTrue(list[0].createdAtEpochMs > 0)
    }

    @Test
    fun listLedgersWhenCreatedAtNullReturns200() {
        val token = login("nullCreatedAt")
        val created = mapper.readValue<LedgerSnapshot>(
            mockMvc.post("/ledgers") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(
                    CreateLedgerRequest(name = "旧本", localId = "local_null_created"),
                )
            }.andExpect { status { isOk() } }.andReturn().response.contentAsString,
        )
        val updated = jdbcTemplate.update("UPDATE ledgers SET created_at = NULL WHERE id = ?", created.id)
        assertEquals(1, updated)
        val listJson = mockMvc.get("/ledgers") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        val list: List<LedgerSummaryDto> = mapper.readValue(listJson)
        val row = list.first { it.id == created.id }
        assertEquals("旧本", row.name)
    }

    @Test
    fun renameLedgerUpdatesName() {
        val token = login("rename1")
        val created = mapper.readValue<LedgerSnapshot>(
            mockMvc.post("/ledgers") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(
                    CreateLedgerRequest(name = "旧名", localId = "local_rename1"),
                )
            }.andExpect { status { isOk() } }.andReturn().response.contentAsString,
        )
        val renamed = mapper.readValue<LedgerSnapshot>(
            mockMvc.patch("/ledgers/${created.id}") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":"新名"}"""
            }.andExpect { status { isOk() } }.andReturn().response.contentAsString,
        )
        assertEquals("新名", renamed.name)
        assertEquals(created.revision + 1, renamed.revision)
        val got = mapper.readValue<LedgerSnapshot>(
            mockMvc.get("/ledgers/${created.id}") {
                header("Authorization", "Bearer $token")
            }.andExpect { status { isOk() } }.andReturn().response.contentAsString,
        )
        assertEquals("新名", got.name)
    }

    private fun login(code: String): String {
        val json = mockMvc.post("/auth/wechat") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(WeChatLoginRequest(code = code, client = "mp"))
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readValue<AuthResponse>(json).token
    }
}
