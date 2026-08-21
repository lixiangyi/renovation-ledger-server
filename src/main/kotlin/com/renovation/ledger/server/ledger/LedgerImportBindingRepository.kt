package com.renovation.ledger.server.ledger

import org.springframework.data.jpa.repository.JpaRepository

interface LedgerImportBindingRepository : JpaRepository<LedgerImportBindingEntity, String> {
    fun findByUserIdAndLocalProjectId(userId: String, localProjectId: String): LedgerImportBindingEntity?
}
