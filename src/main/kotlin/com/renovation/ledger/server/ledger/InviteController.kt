package com.renovation.ledger.server.ledger

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class InviteController(
    private val inviteService: InviteService,
) {
    @PostMapping("/ledgers/{id}/invites")
    fun create(@PathVariable id: String): InviteCreatedDto = inviteService.create(id)

    @DeleteMapping("/ledgers/{id}/invites/{inviteId}")
    fun revoke(@PathVariable id: String, @PathVariable inviteId: String) {
        inviteService.revoke(id, inviteId)
    }

    @PostMapping("/invites/join")
    fun join(@RequestBody request: JoinInviteRequest): LedgerSnapshot = inviteService.join(request)

    @GetMapping("/ledgers/{id}/members")
    fun members(@PathVariable id: String): List<MemberDto> = inviteService.listMembers(id)
}
