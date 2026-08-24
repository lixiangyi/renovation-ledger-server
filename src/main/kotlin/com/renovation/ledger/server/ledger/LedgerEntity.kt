package com.renovation.ledger.server.ledger

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ledgers")
class LedgerEntity(
    @Id val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var revision: Long = 0,
    var ownerUserId: String = "",
    var createdAt: Instant = Instant.now(),
    var deletedAt: Instant? = null,
)
