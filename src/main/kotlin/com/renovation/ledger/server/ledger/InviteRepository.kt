package com.renovation.ledger.server.ledger

import org.springframework.data.jpa.repository.JpaRepository

interface InviteRepository : JpaRepository<InviteEntity, String> {
    fun findByCode(code: String): InviteEntity?
    fun findAllByLedgerId(ledgerId: String): List<InviteEntity>
}
