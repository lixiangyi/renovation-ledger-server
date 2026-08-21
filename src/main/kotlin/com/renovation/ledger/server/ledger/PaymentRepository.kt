package com.renovation.ledger.server.ledger

import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository : JpaRepository<PaymentRow, String> {
    fun findAllByBudgetItemId(budgetItemId: String): List<PaymentRow>
    fun deleteAllByBudgetItemId(budgetItemId: String)
}
