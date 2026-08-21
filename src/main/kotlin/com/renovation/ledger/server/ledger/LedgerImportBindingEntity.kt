package com.renovation.ledger.server.ledger

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    name = "ledger_import_bindings",
    uniqueConstraints = [jakarta.persistence.UniqueConstraint(columnNames = ["userId", "localProjectId"])],
)
class LedgerImportBindingEntity(
    @Id val id: String = UUID.randomUUID().toString(),
    var userId: String = "",
    var localProjectId: String = "",
    var ledgerId: String = "",
)
