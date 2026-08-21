package com.renovation.ledger.server.ledger

import com.renovation.ledger.server.error.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ItemSyncService(
    private val ledgers: LedgerRepository,
    private val ledgerService: LedgerService,
    private val items: BudgetItemRepository,
    private val payments: PaymentRepository,
) {
    @Transactional
    fun upsert(ledgerId: String, itemId: String, request: PutItemRequest): LedgerSnapshot {
        val userId = ledgerService.currentUserId()
        ledgerService.requireMember(userId, ledgerId)
        val ledger = ledgers.findByIdForUpdate(ledgerId)
            ?: throw ApiException(403, "FORBIDDEN", "没有这个账本的权限")
        val item = request.item.copy(id = itemId)
        ledgerService.writeItem(ledgerId, item)
        ledger.revision += 1
        ledgers.save(ledger)
        return ledgerService.snapshot(ledgerId)
    }

    @Transactional
    fun delete(ledgerId: String, itemId: String, baseRevision: Long): LedgerSnapshot {
        val userId = ledgerService.currentUserId()
        ledgerService.requireMember(userId, ledgerId)
        val ledger = ledgers.findByIdForUpdate(ledgerId)
            ?: throw ApiException(403, "FORBIDDEN", "没有这个账本的权限")
        if (items.existsById(itemId)) {
            payments.deleteAllByBudgetItemId(itemId)
            items.deleteById(itemId)
            ledger.revision += 1
            ledgers.save(ledger)
        }
        return ledgerService.snapshot(ledgerId)
    }
}
