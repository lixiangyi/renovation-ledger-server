package com.renovation.ledger.server.ledger

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface LedgerRepository : JpaRepository<LedgerEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LedgerEntity l where l.id = :id")
    fun findByIdForUpdate(id: String): LedgerEntity?
}
