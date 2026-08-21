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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class LedgerImportTest {
    @TestConfiguration
    class WeChatStubConfig {
        @Bean
        @Primary
        fun weChatClient(): WeChatClient = StubWeChatClient()
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper

    @Test
    fun importThenGetRoundTrip() {
        val token = login("u1")
        val body = sample("proj_1")
        val created = import(token, body)
        val fetched = getLedger(token, created.id)
        assertEquals("我家装修", fetched.name)
        assertEquals(1, fetched.items.size)
        assertEquals("灯具", fetched.items[0].name)
    }

    @Test
    fun importSameLocalIdTwiceDoesNotDuplicate() {
        val token = login("u2")
        val a = import(token, sample("proj_1"))
        val b = import(token, sample("proj_1"))
        assertEquals(a.id, b.id)
        val list: List<LedgerSummaryDto> = mapper.readValue(
            mockMvc.get("/ledgers") {
                header("Authorization", "Bearer $token")
            }.andExpect { status { isOk() } }.andReturn().response.contentAsString,
        )
        assertEquals(1, list.size)
    }

    private fun login(code: String): String {
        val json = mockMvc.post("/auth/wechat") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(WeChatLoginRequest(code = code, client = "mp"))
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readValue<AuthResponse>(json).token
    }

    private fun import(token: String, body: ImportLedgerRequest): LedgerSnapshot {
        val json = mockMvc.post("/ledgers/import") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(body)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readValue(json)
    }

    private fun getLedger(token: String, id: String): LedgerSnapshot {
        val json = mockMvc.get("/ledgers/$id") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readValue(json)
    }

    private fun sample(localId: String) = ImportLedgerRequest(
        localId = localId,
        name = "我家装修",
        items = listOf(
            ItemDto(
                id = "item_1",
                name = "灯具",
                stage = "软装",
                category = "灯具",
                space = "客厅",
                budgetAmount = 10000,
                contractAmount = null,
                merchant = "",
                recordedDate = null,
                remark = "",
                isNewAddition = false,
                payments = emptyList(),
            ),
        ),
        taxonomy = TaxonomyDto(
            stages = listOf("软装"),
            categories = listOf("灯具"),
            spaces = listOf("客厅"),
            iconsJson = "{}",
        ),
    )
}
