package com.renovation.ledger.server.ledger

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "ledger_taxonomy")
class LedgerTaxonomyEntity(
    @Id var ledgerId: String = "",
    var stagesJson: String = "[]",
    var categoriesJson: String = "[]",
    var spacesJson: String = "[]",
    var iconsJson: String = "{}",
)
