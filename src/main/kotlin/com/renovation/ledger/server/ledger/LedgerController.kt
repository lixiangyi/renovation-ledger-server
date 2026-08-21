package com.renovation.ledger.server.ledger

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class LedgerController(
    private val ledgerService: LedgerService,
    private val itemSyncService: ItemSyncService,
) {
    @GetMapping("/ledgers")
    fun list(): List<LedgerSummaryDto> = ledgerService.listLedgers()

    @PostMapping("/ledgers")
    fun create(@RequestBody request: CreateLedgerRequest): LedgerSnapshot =
        ledgerService.createLedger(request)

    @PostMapping("/ledgers/import")
    fun import(@RequestBody request: ImportLedgerRequest): LedgerSnapshot =
        ledgerService.importLedger(request)

    @GetMapping("/ledgers/{id}")
    fun get(@PathVariable id: String): LedgerSnapshot = ledgerService.getLedger(id)

    @PatchMapping("/ledgers/{id}")
    fun rename(
        @PathVariable id: String,
        @RequestBody request: RenameLedgerRequest,
    ): LedgerSnapshot = ledgerService.rename(id, request.name)

    @PutMapping("/ledgers/{id}/items/{itemId}")
    fun putItem(
        @PathVariable id: String,
        @PathVariable itemId: String,
        @RequestBody request: PutItemRequest,
    ): LedgerSnapshot = itemSyncService.upsert(id, itemId, request)

    @DeleteMapping("/ledgers/{id}/items/{itemId}")
    fun deleteItem(
        @PathVariable id: String,
        @PathVariable itemId: String,
        @RequestParam baseRevision: Long,
    ): LedgerSnapshot = itemSyncService.delete(id, itemId, baseRevision)

    @DeleteMapping("/ledgers/{id}/members/{userId}")
    fun kick(@PathVariable id: String, @PathVariable userId: String) {
        ledgerService.kick(id, userId)
    }

    @PostMapping("/ledgers/{id}/leave")
    fun leave(@PathVariable id: String) {
        ledgerService.leave(id)
    }

    @PostMapping("/ledgers/{id}/transfer")
    fun transfer(@PathVariable id: String, @RequestBody request: TransferRequest) {
        ledgerService.transfer(id, request.userId)
    }

    @DeleteMapping("/ledgers/{id}")
    fun deleteLedger(@PathVariable id: String) {
        ledgerService.softDelete(id)
    }

    @PostMapping("/ledgers/{id}/restore")
    fun restore(@PathVariable id: String) {
        ledgerService.restore(id)
    }
}
