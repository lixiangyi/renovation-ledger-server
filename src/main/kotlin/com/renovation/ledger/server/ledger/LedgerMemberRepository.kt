package com.renovation.ledger.server.ledger

import org.springframework.data.jpa.repository.JpaRepository

interface LedgerMemberRepository : JpaRepository<LedgerMemberEntity, String> {
    fun findByLedgerIdAndUserId(ledgerId: String, userId: String): LedgerMemberEntity?
    fun findAllByLedgerId(ledgerId: String): List<LedgerMemberEntity>
    fun findAllByUserId(userId: String): List<LedgerMemberEntity>
    fun deleteByLedgerIdAndUserId(ledgerId: String, userId: String)
}
