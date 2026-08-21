package com.renovation.ledger.server.ledger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.renovation.ledger.server.auth.AuthResponse
import com.renovation.ledger.server.auth.WeChatLoginRequest
import com.renovation.ledger.server.wechat.StubWeChatClient
import com.renovation.ledger.server.wechat.WeChatClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
class InviteServiceTest {
    @TestConfiguration
    class WeChatStubConfig {
        @Bean
        @Primary
        fun weChatClient(): WeChatClient = StubWeChatClient()
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper
    @Autowired lateinit var invites: InviteRepository

    @Test
    fun ownerCreatesInviteEditorJoins() {
        val owner = login("inv_owner")
        val editor = login("inv_editor")
        val ledger = import(owner, "p_inv")
        val invite = createInvite(owner, ledger.id)
        mockMvc.post("/invites/join") {
            header("Authorization", "Bearer $editor")
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(JoinInviteRequest(invite.code))
        }.andExpect { status { isOk() } }
        val members: List<MemberDto> = mapper.readValue(
            mockMvc.get("/ledgers/${ledger.id}/members") {
                header("Authorization", "Bearer $owner")
            }.andExpect { status { isOk() } }.andReturn().response.contentAsString,
        )
        assertEquals(2, members.size)
        assertTrue(members.any { it.role == "EDITOR" })
    }

    @Test
    fun expiredInviteIs410() {
        val owner = login("inv_exp_o")
        val editor = login("inv_exp_e")
        val ledger = import(owner, "p_exp")
        val invite = createInvite(owner, ledger.id)
        val entity = invites.findByCode(invite.code)!!
        entity.expiresAt = java.time.Instant.now().minusSeconds(60)
        invites.save(entity)
        mockMvc.post("/invites/join") {
            header("Authorization", "Bearer $editor")
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(JoinInviteRequest(invite.code))
        }.andExpect { status { isGone() } }
    }

    @Test
    fun editorCannotCreateInvite() {
        val owner = login("inv_ed_o")
        val editor = login("inv_ed_e")
        val ledger = import(owner, "p_ed")
        val invite = createInvite(owner, ledger.id)
        mockMvc.post("/invites/join") {
            header("Authorization", "Bearer $editor")
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(JoinInviteRequest(invite.code))
        }.andExpect { status { isOk() } }
        mockMvc.post("/ledgers/${ledger.id}/invites") {
            header("Authorization", "Bearer $editor")
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun joinTwiceIsIdempotent() {
        val owner = login("inv_twice_o")
        val editor = login("inv_twice_e")
        val ledger = import(owner, "p_twice")
        val invite = createInvite(owner, ledger.id)
        repeat(2) {
            mockMvc.post("/invites/join") {
                header("Authorization", "Bearer $editor")
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(JoinInviteRequest(invite.code))
            }.andExpect { status { isOk() } }
        }
        val members: List<MemberDto> = mapper.readValue(
            mockMvc.get("/ledgers/${ledger.id}/members") {
                header("Authorization", "Bearer $owner")
            }.andExpect { status { isOk() } }.andReturn().response.contentAsString,
        )
        assertEquals(2, members.size)
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
            content = mapper.writeValueAsString(
                ImportLedgerRequest(localId = localId, name = "L", items = emptyList()),
            )
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readValue(json)
    }

    private fun createInvite(token: String, ledgerId: String): InviteCreatedDto {
        val json = mockMvc.post("/ledgers/$ledgerId/invites") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return mapper.readValue(json)
    }
}
