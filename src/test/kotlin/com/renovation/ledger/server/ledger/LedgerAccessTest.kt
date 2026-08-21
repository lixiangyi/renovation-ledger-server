package com.renovation.ledger.server.ledger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.renovation.ledger.server.auth.AuthResponse
import com.renovation.ledger.server.auth.WeChatLoginRequest
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
class LedgerAccessTest {
    @TestConfiguration
    class WeChatStubConfig {
        @Bean
        @Primary
        fun weChatClient(): WeChatClient = StubWeChatClient()
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper

    @Test
    fun otherUserCannotReadOrWrite() {
        val owner = login("ownerA")
        val stranger = login("strangerB")
        val ledger = import(owner)
        mockMvc.get("/ledgers/${ledger.id}") {
            header("Authorization", "Bearer $stranger")
        }.andExpect { status { isForbidden() } }
        mockMvc.put("/ledgers/${ledger.id}/items/${ledger.items[0].id}") {
            header("Authorization", "Bearer $stranger")
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(PutItemRequest(ledger.revision, ledger.items[0]))
        }.andExpect { status { isForbidden() } }
    }

    private fun login(code: String): String {
        val json = mockMvc.post("/auth/wechat") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(WeChatLoginRequest(code = code, client = "mp"))
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readValue<AuthResponse>(json).token
    }

    private fun import(token: String): LedgerSnapshot {
        val json = mockMvc.post("/ledgers/import") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(
                ImportLedgerRequest(
                    localId = "p_access",
                    name = "A",
                    items = listOf(ItemDto(id = "i1", name = "x", stage = "s", category = "c", space = "sp", budgetAmount = 1)),
                ),
            )
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readValue(json)
    }
}
