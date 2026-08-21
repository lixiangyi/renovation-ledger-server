package com.renovation.ledger.server.ledger

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "invites")
class InviteEntity(
    @Id val id: String = UUID.randomUUID().toString(),
    var ledgerId: String = "",
    var code: String = "",
    var expiresAt: Instant = Instant.now(),
    var revokedAt: Instant? = null,
    var createdBy: String = "",
)
