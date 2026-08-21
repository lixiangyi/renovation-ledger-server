package com.renovation.ledger.server.ledger

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "budget_items")
class BudgetItemRow(
    @Id val id: String = UUID.randomUUID().toString(),
    var ledgerId: String = "",
    var name: String = "",
    var stage: String = "",
    var category: String = "",
    var space: String = "",
    var budgetAmount: Long = 0,
    var contractAmount: Long? = null,
    var merchant: String = "",
    var recordedDate: String? = null,
    var remark: String = "",
    var isNewAddition: Boolean = false,
    var settledOnDate: String? = null,
    var settledAtEpochMs: Long? = null,
    var updatedAt: Instant = Instant.now(),
)
