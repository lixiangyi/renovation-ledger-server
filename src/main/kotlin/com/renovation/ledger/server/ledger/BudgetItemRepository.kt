package com.renovation.ledger.server.ledger

import org.springframework.data.jpa.repository.JpaRepository

interface BudgetItemRepository : JpaRepository<BudgetItemRow, String> {
    fun findAllByLedgerId(ledgerId: String): List<BudgetItemRow>
    fun deleteAllByLedgerId(ledgerId: String)
}
