package com.renovation.ledger.server.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, String> {
    fun findByPhone(phone: String): UserEntity?
}
