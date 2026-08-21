package com.renovation.ledger.server.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id val id: String = UUID.randomUUID().toString(),
    var nickname: String = "我",
    var avatarUrl: String? = null,
    @Column(unique = true) var phone: String? = null,
)
