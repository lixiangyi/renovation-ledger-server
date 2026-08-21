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
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
class ItemSyncServiceTest {
    @TestConfiguration
    class WeChatStubConfig {
        @Bean
        @Primary
        fun weChatClient(): WeChatClient = StubWeChatClient()
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper

    @Test
    fun putItemIncrementsLedgerRevision() {
        val token = login("sync1")
        val ledger = import(token, "proj_sync1")
        val updated = ledger.items[0].copy(name = "灯具-改")
        putItem(token, ledger.id, updated, ledger.revision, expectedStatus = 200)
        val again = getLedger(token, ledger.id)
        assertEquals("灯具-改", again.items[0].name)
        assertEquals(ledger.revision + 1, again.revision)
    }

    @Test
    fun sameItemLastWriteWins() {
        val token = login("sync2")
        val ledger = import(token, "proj_sync2")
        putItem(token, ledger.id, ledger.items[0].copy(name = "A"), ledger.revision, 200)
        putItem(token, ledger.id, ledger.items[0].copy(name = "B"), ledger.revision, 200)
        val again = getLedger(token, ledger.id)
        assertEquals("B", again.items[0].name)
    }

    @Test
    fun putItemPersistsOperationTimes() {
        val token = login("sync_times")
        val ledger = import(token, "proj_sync_times")
        val updated = ledger.items[0].copy(
            settledOnDate = "2026-03-16",
            settledAtEpochMs = 1_773_640_320_000L,
            payments = listOf(
                PaymentDto(
                    id = "pay_1",
                    type = "FINAL",
                    amount = 10000,
                    status = "PAID",
                    paidAtEpochMs = 1_773_640_320_000L,
                    paidOnDate = "2026-03-15",
                    createdByName = "我",
                ),
            ),
        )
        putItem(token, ledger.id, updated, ledger.revision, 200)
        val again = getLedger(token, ledger.id)
        assertEquals("2026-03-16", again.items[0].settledOnDate)
        assertEquals(1_773_640_320_000L, again.items[0].settledAtEpochMs)
        assertEquals("2026-03-15", again.items[0].payments[0].paidOnDate)
        assertEquals(1_773_640_320_000L, again.items[0].payments[0].paidAtEpochMs)
    }

    @Test
    fun differentItemsBothSucceed() {
        val token = login("sync3")
        val first = import(token, "proj_sync3")
        val second = ItemDto(
            id = "item_2",
            name = "地板",
            stage = "硬装",
            category = "地面",
            space = "客厅",
            budgetAmount = 20000,
        )
        putItem(token, first.id, second, first.revision, 200)
        putItem(token, first.id, first.items[0].copy(name = "灯具-甲"), first.revision, 200)
        val again = getLedger(token, first.id)
        assertEquals(2, again.items.size)
        assertEquals("灯具-甲", again.items.first { it.id == "item_1" }.name)
        assertEquals("地板", again.items.first { it.id == "item_2" }.name)
    }

    private fun login(code: String): String {
        val json = mockMvc.post("/auth/wechat") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(WeChatLoginRequest(code = code, client = "mp"))
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readValue<AuthResponse>(json).token
    }

    private fun import(token: String, localId: String): LedgerSnapshot {
        val body = ImportLedgerRequest(
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
                ),
            ),
        )
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

    private fun putItem(token: String, ledgerId: String, item: ItemDto, baseRevision: Long, expectedStatus: Int) {
        mockMvc.put("/ledgers/$ledgerId/items/${item.id}") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(PutItemRequest(baseRevision = baseRevision, item = item))
        }.andExpect { status { isEqualTo(expectedStatus) } }
    }
}
