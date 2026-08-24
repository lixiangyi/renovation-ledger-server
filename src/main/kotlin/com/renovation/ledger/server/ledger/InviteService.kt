package com.renovation.ledger.server.ledger

import com.renovation.ledger.server.error.ApiException
import com.renovation.ledger.server.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

@Service
class InviteService(
    private val ledgerService: LedgerService,
    private val invites: InviteRepository,
    private val members: LedgerMemberRepository,
    private val users: UserRepository,
    private val ledgers: LedgerRepository,
) {
    private val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    @Transactional
    fun create(ledgerId: String): InviteCreatedDto {
        val userId = ledgerService.currentUserId()
        ledgerService.requireOwner(userId, ledgerId)
        val code = generateCode()
        val saved = invites.save(
            InviteEntity(
                ledgerId = ledgerId,
                code = code,
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS),
                createdBy = userId,
            ),
        )
        return InviteCreatedDto(id = saved.id, code = saved.code)
    }

    @Transactional
    fun revoke(ledgerId: String, inviteId: String) {
        val userId = ledgerService.currentUserId()
        ledgerService.requireOwner(userId, ledgerId)
        val invite = invites.findById(inviteId).orElseThrow {
            ApiException(410, "GONE", "邀请已失效")
        }
        invite.revokedAt = Instant.now()
        invites.save(invite)
    }

    fun preview(code: String): InvitePreviewDto {
        val userId = ledgerService.currentUserId()
        val invite = requireValidInvite(code)
        val ledger = ledgers.findById(invite.ledgerId).orElse(null)
            ?: throw ApiException(410, "GONE", "邀请已失效")
        if (ledger.deletedAt != null) {
            throw ApiException(410, "GONE", "邀请已失效")
        }
        val ownerMember = members.findAllByLedgerId(invite.ledgerId)
            .firstOrNull { it.role.equals("OWNER", ignoreCase = true) }
        val ownerNickname = ownerMember?.let { m ->
            users.findById(m.userId).orElse(null)?.nickname?.takeIf { it.isNotBlank() }
        } ?: "账本拥有者"
        val alreadyMember = members.findByLedgerIdAndUserId(invite.ledgerId, userId) != null
        return InvitePreviewDto(
            code = invite.code,
            ledgerId = ledger.id,
            ledgerName = ledger.name,
            ownerNickname = ownerNickname,
            alreadyMember = alreadyMember,
        )
    }

    @Transactional
    fun join(request: JoinInviteRequest): LedgerSnapshot {
        val userId = ledgerService.currentUserId()
        val invite = requireValidInvite(request.code)
        val existing = members.findByLedgerIdAndUserId(invite.ledgerId, userId)
        if (existing == null) {
            members.save(
                LedgerMemberEntity(
                    ledgerId = invite.ledgerId,
                    userId = userId,
                    role = "EDITOR",
                ),
            )
        }
        return ledgerService.snapshot(invite.ledgerId)
    }

    fun listMembers(ledgerId: String): List<MemberDto> {
        ledgerService.requireMember(ledgerService.currentUserId(), ledgerId)
        return members.findAllByLedgerId(ledgerId).map { m ->
            val nick = users.findById(m.userId).orElse(null)?.nickname ?: "我"
            MemberDto(userId = m.userId, nickname = nick, role = m.role)
        }
    }

    private fun requireValidInvite(code: String): InviteEntity {
        val invite = invites.findByCode(code.trim())
            ?: throw ApiException(410, "GONE", "邀请已失效")
        if (invite.revokedAt != null || invite.expiresAt.isBefore(Instant.now())) {
            throw ApiException(410, "GONE", "邀请已失效")
        }
        return invite
    }

    private fun generateCode(): String {
        repeat(20) {
            val code = CharArray(6) { alphabet[Random.nextInt(alphabet.length)] }.concatToString()
            if (invites.findByCode(code) == null) return code
        }
        error("could not allocate invite code")
    }
}
