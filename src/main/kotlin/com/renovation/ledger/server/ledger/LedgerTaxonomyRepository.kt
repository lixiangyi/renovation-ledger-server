package com.renovation.ledger.server.ledger

import org.springframework.data.jpa.repository.JpaRepository

interface LedgerTaxonomyRepository : JpaRepository<LedgerTaxonomyEntity, String>
