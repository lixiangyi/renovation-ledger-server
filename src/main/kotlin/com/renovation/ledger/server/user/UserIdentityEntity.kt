package com.renovation.ledger.server.user

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "user_identities",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "openid"])],
)
class UserIdentityEntity(
    @Id val id: String = UUID.randomUUID().toString(),
    var userId: String,
    var provider: String,
    var openid: String,
    var unionid: String? = null,
)
