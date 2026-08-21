package com.renovation.ledger.server.ledger

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "payments")
class PaymentRow(
    @Id val id: String = UUID.randomUUID().toString(),
    var budgetItemId: String = "",
    var type: String = "OTHER",
    var amount: Long = 0,
    var status: String = "UNPAID",
    var paidAtEpochMs: Long? = null,
    var paidOnDate: String? = null,
    var note: String = "",
    var receiptUri: String? = null,
    var createdByUserId: String? = null,
    var createdByName: String = "",
)
