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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class MemberLifecycleTest {
    @TestConfiguration
    class WeChatStubConfig {
        @Bean
        @Primary
        fun weChatClient(): WeChatClient = StubWeChatClient()
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper
    @Autowired lateinit var authService: com.renovation.ledger.server.auth.AuthService

    @Test
    fun kickThenGetIs403() {
        val ownerTok = login("life_o")
        val editorTok = login("life_e")
        val editorId = authService.loginWeChat(WeChatLoginRequest("life_e", "mp")).userId
        val ledger = import(ownerTok, "p_kick")
        join(editorTok, createInvite(ownerTok, ledger.id).code)
        mockMvc.delete("/ledgers/${ledger.id}/members/$editorId") {
            header("Authorization", "Bearer $ownerTok")
        }.andExpect { status { isOk() } }
        mockMvc.get("/ledgers/${ledger.id}") {
            header("Authorization", "Bearer $editorTok")
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun ownerLeaveIs400() {
        val ownerTok = login("life_leave_o")
        val ledger = import(ownerTok, "p_leave")
        mockMvc.post("/ledgers/${ledger.id}/leave") {
            header("Authorization", "Bearer $ownerTok")
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun transferThenOldOwnerCannotDelete() {
        val ownerTok = login("life_tr_o")
        val editorTok = login("life_tr_e")
        val editorId = authService.loginWeChat(WeChatLoginRequest("life_tr_e", "mp")).userId
        val ledger = import(ownerTok, "p_tr")
        join(editorTok, createInvite(ownerTok, ledger.id).code)
        mockMvc.post("/ledgers/${ledger.id}/transfer") {
            header("Authorization", "Bearer $ownerTok")
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(TransferRequest(editorId))
        }.andExpect { status { isOk() } }
        mockMvc.delete("/ledgers/${ledger.id}") {
            header("Authorization", "Bearer $ownerTok")
        }.andExpect { status { isForbidden() } }
        mockMvc.delete("/ledgers/${ledger.id}") {
            header("Authorization", "Bearer $editorTok")
        }.andExpect { status { isOk() } }
        mockMvc.get("/ledgers/${ledger.id}") {
            header("Authorization", "Bearer $editorTok")
        }.andExpect { status { isForbidden() } }
        mockMvc.post("/ledgers/${ledger.id}/restore") {
            header("Authorization", "Bearer $editorTok")
        }.andExpect { status { isOk() } }
        mockMvc.get("/ledgers/${ledger.id}") {
            header("Authorization", "Bearer $editorTok")
        }.andExpect { status { isOk() } }
    }

    private fun login(code: String): String {
        val json = mockMvc.post("/auth/wechat") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(WeChatLoginRequest(code = code, client = "mp"))
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readValue<AuthResponse>(json).token
    }

    private fun import(token: String, localId: String): LedgerSnapshot {
        val json = mockMvc.post("/ledgers/import") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(ImportLedgerRequest(localId = localId, name = "L"))
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readValue(json)
    }

    private fun createInvite(token: String, ledgerId: String): InviteCreatedDto {
        val json = mockMvc.post("/ledgers/$ledgerId/invites") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readValue(json)
    }

    private fun join(token: String, code: String) {
        mockMvc.post("/invites/join") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(JoinInviteRequest(code))
        }.andExpect { status { isOk() } }
    }
}
