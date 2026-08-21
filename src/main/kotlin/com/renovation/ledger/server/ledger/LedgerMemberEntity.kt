package com.renovation.ledger.server.ledger

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "ledger_members")
class LedgerMemberEntity(
    @Id val id: String = UUID.randomUUID().toString(),
    var ledgerId: String = "",
    var userId: String = "",
    var role: String = "EDITOR",
)
