package com.renovation.ledger.server.ledger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.renovation.ledger.server.error.ApiException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LedgerService(
    private val ledgers: LedgerRepository,
    private val members: LedgerMemberRepository,
    private val items: BudgetItemRepository,
    private val payments: PaymentRepository,
    private val taxonomyRepo: LedgerTaxonomyRepository,
    private val bindings: LedgerImportBindingRepository,
    private val mapper: ObjectMapper,
) {
    fun currentUserId(): String =
        SecurityContextHolder.getContext().authentication?.name
            ?: throw ApiException(401, "UNAUTHENTICATED", "请重新登录")

    fun requireMember(userId: String, ledgerId: String): LedgerMemberEntity {
        val ledger = ledgers.findById(ledgerId).orElse(null)
            ?: throw ApiException(403, "FORBIDDEN", "没有这个账本的权限")
        if (ledger.deletedAt != null) {
            throw ApiException(403, "FORBIDDEN", "没有这个账本的权限")
        }
        return members.findByLedgerIdAndUserId(ledgerId, userId)
            ?: throw ApiException(403, "FORBIDDEN", "没有这个账本的权限")
    }

    fun requireOwner(userId: String, ledgerId: String): LedgerMemberEntity {
        val member = requireMember(userId, ledgerId)
        if (member.role != "OWNER") {
            throw ApiException(403, "FORBIDDEN", "没有这个账本的权限")
        }
        return member
    }

    @Transactional
    fun importLedger(request: ImportLedgerRequest): LedgerSnapshot {
        val userId = currentUserId()
        val existing = bindings.findByUserIdAndLocalProjectId(userId, request.localId)
        if (existing != null) {
            return snapshot(existing.ledgerId)
        }
        val ledger = ledgers.save(
            LedgerEntity(name = request.name, revision = 0, ownerUserId = userId),
        )
        members.save(
            LedgerMemberEntity(ledgerId = ledger.id, userId = userId, role = "OWNER"),
        )
        bindings.save(
            LedgerImportBindingEntity(
                userId = userId,
                localProjectId = request.localId,
                ledgerId = ledger.id,
            ),
        )
        writeTaxonomy(ledger.id, request.taxonomy)
        request.items.forEach { writeItem(ledger.id, it) }
        return snapshot(ledger.id)
    }

    @Transactional
    fun createLedger(request: CreateLedgerRequest): LedgerSnapshot {
        val userId = currentUserId()
        val existing = bindings.findByUserIdAndLocalProjectId(userId, request.localId)
        if (existing != null) {
            return snapshot(existing.ledgerId)
        }
        val name = request.name.trim().ifBlank { "新账本" }
        val ledger = ledgers.save(
            LedgerEntity(name = name, revision = 0, ownerUserId = userId),
        )
        members.save(
            LedgerMemberEntity(ledgerId = ledger.id, userId = userId, role = "OWNER"),
        )
        bindings.save(
            LedgerImportBindingEntity(
                userId = userId,
                localProjectId = request.localId,
                ledgerId = ledger.id,
            ),
        )
        writeTaxonomy(ledger.id, TaxonomyDto())
        return snapshot(ledger.id)
    }

    fun listLedgers(): List<LedgerSummaryDto> {
        val userId = currentUserId()
        return members.findAllByUserId(userId).mapNotNull { member ->
            val ledger = ledgers.findById(member.ledgerId).orElse(null) ?: return@mapNotNull null
            if (ledger.deletedAt != null) return@mapNotNull null
            LedgerSummaryDto(
                id = ledger.id,
                name = ledger.name,
                role = member.role,
                revision = ledger.revision,
                createdAtEpochMs = ledger.createdAt?.toEpochMilli() ?: 0L,
            )
        }.sortedBy { it.createdAtEpochMs }
    }

    fun getLedger(id: String): LedgerSnapshot {
        requireMember(currentUserId(), id)
        return snapshot(id)
    }

    fun snapshot(ledgerId: String): LedgerSnapshot {
        val ledger = ledgers.findById(ledgerId).orElseThrow()
        val itemRows = items.findAllByLedgerId(ledgerId)
        val tax = taxonomyRepo.findById(ledgerId).orElse(null)
        return LedgerSnapshot(
            id = ledger.id,
            name = ledger.name,
            revision = ledger.revision,
            items = itemRows.map { row ->
                toDto(row, payments.findAllByBudgetItemId(row.id))
            },
            taxonomy = TaxonomyDto(
                stages = tax?.stagesJson?.let { mapper.readValue(it) } ?: emptyList(),
                categories = tax?.categoriesJson?.let { mapper.readValue(it) } ?: emptyList(),
                spaces = tax?.spacesJson?.let { mapper.readValue(it) } ?: emptyList(),
                iconsJson = tax?.iconsJson ?: "{}",
            ),
        )
    }

    fun writeTaxonomy(ledgerId: String, dto: TaxonomyDto) {
        taxonomyRepo.save(
            LedgerTaxonomyEntity(
                ledgerId = ledgerId,
                stagesJson = mapper.writeValueAsString(dto.stages),
                categoriesJson = mapper.writeValueAsString(dto.categories),
                spacesJson = mapper.writeValueAsString(dto.spaces),
                iconsJson = dto.iconsJson,
            ),
        )
    }

    @Transactional
    fun kick(ledgerId: String, targetUserId: String) {
        val userId = currentUserId()
        requireOwner(userId, ledgerId)
        val target = members.findByLedgerIdAndUserId(ledgerId, targetUserId)
            ?: throw ApiException(403, "FORBIDDEN", "没有这个账本的权限")
        if (target.role == "OWNER") {
            throw ApiException(400, "BAD_REQUEST", "不能移除所有者")
        }
        members.deleteByLedgerIdAndUserId(ledgerId, targetUserId)
    }

    @Transactional
    fun leave(ledgerId: String) {
        val userId = currentUserId()
        val member = requireMember(userId, ledgerId)
        if (member.role == "OWNER") {
            throw ApiException(400, "BAD_REQUEST", "请先转让")
        }
        members.deleteByLedgerIdAndUserId(ledgerId, userId)
    }

    @Transactional
    fun transfer(ledgerId: String, newOwnerId: String) {
        val userId = currentUserId()
        requireOwner(userId, ledgerId)
        val incoming = members.findByLedgerIdAndUserId(ledgerId, newOwnerId)
            ?: throw ApiException(400, "BAD_REQUEST", "对方不是成员")
        val current = members.findByLedgerIdAndUserId(ledgerId, userId)!!
        incoming.role = "OWNER"
        current.role = "EDITOR"
        members.save(incoming)
        members.save(current)
        val ledger = ledgers.findById(ledgerId).orElseThrow()
        ledger.ownerUserId = newOwnerId
        ledgers.save(ledger)
    }

    @Transactional
    fun softDelete(ledgerId: String) {
        val userId = currentUserId()
        requireOwner(userId, ledgerId)
        val ledger = ledgers.findById(ledgerId).orElseThrow()
        ledger.deletedAt = java.time.Instant.now()
        ledgers.save(ledger)
    }

    @Transactional
    fun restore(ledgerId: String) {
        val userId = currentUserId()
        val ledger = ledgers.findById(ledgerId).orElseThrow {
            ApiException(403, "FORBIDDEN", "没有这个账本的权限")
        }
        val member = members.findByLedgerIdAndUserId(ledgerId, userId)
            ?: throw ApiException(403, "FORBIDDEN", "没有这个账本的权限")
        if (member.role != "OWNER") {
            throw ApiException(403, "FORBIDDEN", "没有这个账本的权限")
        }
        val deletedAt = ledger.deletedAt ?: throw ApiException(400, "BAD_REQUEST", "账本未删除")
        if (deletedAt.isBefore(java.time.Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS))) {
            throw ApiException(410, "GONE", "无法恢复")
        }
        ledger.deletedAt = null
        ledgers.save(ledger)
    }

    @Transactional
    fun rename(ledgerId: String, name: String): LedgerSnapshot {
        requireOwner(currentUserId(), ledgerId)
        val ledger = ledgers.findById(ledgerId).orElseThrow()
        ledger.name = name.trim().ifBlank { "新账本" }
        ledger.revision += 1
        ledgers.save(ledger)
        return snapshot(ledgerId)
    }

    fun writeItem(ledgerId: String, dto: ItemDto) {
        val existing = items.findById(dto.id).orElse(null)
        val row = existing?.apply {
            this.ledgerId = ledgerId
            name = dto.name
            stage = dto.stage
            category = dto.category
            space = dto.space
            budgetAmount = dto.budgetAmount
            contractAmount = dto.contractAmount
            merchant = dto.merchant
            recordedDate = dto.recordedDate
            remark = dto.remark
            isNewAddition = dto.isNewAddition
            settledOnDate = dto.settledOnDate
            settledAtEpochMs = dto.settledAtEpochMs
            updatedAt = java.time.Instant.now()
        } ?: BudgetItemRow(
            id = dto.id,
            ledgerId = ledgerId,
            name = dto.name,
            stage = dto.stage,
            category = dto.category,
            space = dto.space,
            budgetAmount = dto.budgetAmount,
            contractAmount = dto.contractAmount,
            merchant = dto.merchant,
            recordedDate = dto.recordedDate,
            remark = dto.remark,
            isNewAddition = dto.isNewAddition,
            settledOnDate = dto.settledOnDate,
            settledAtEpochMs = dto.settledAtEpochMs,
        )
        items.save(row)
        payments.deleteAllByBudgetItemId(dto.id)
        dto.payments.forEach { p ->
            payments.save(
                PaymentRow(
                    id = p.id,
                    budgetItemId = dto.id,
                    type = p.type,
                    amount = p.amount,
                    status = p.status,
                    paidAtEpochMs = p.paidAtEpochMs,
                    paidOnDate = p.paidOnDate,
                    note = p.note,
                    receiptUri = p.receiptUri,
                    createdByUserId = p.createdByUserId,
                    createdByName = p.createdByName,
                ),
            )
        }
    }

    private fun toDto(row: BudgetItemRow, pays: List<PaymentRow>) = ItemDto(
        id = row.id,
        name = row.name,
        stage = row.stage,
        category = row.category,
        space = row.space,
        budgetAmount = row.budgetAmount,
        contractAmount = row.contractAmount,
        merchant = row.merchant,
        recordedDate = row.recordedDate,
        remark = row.remark,
        isNewAddition = row.isNewAddition,
        settledOnDate = row.settledOnDate,
        settledAtEpochMs = row.settledAtEpochMs,
        payments = pays.map {
            PaymentDto(
                id = it.id,
                type = it.type,
                amount = it.amount,
                status = it.status,
                paidAtEpochMs = it.paidAtEpochMs,
                paidOnDate = it.paidOnDate,
                note = it.note,
                receiptUri = it.receiptUri,
                createdByUserId = it.createdByUserId,
                createdByName = it.createdByName,
            )
        },
    )
}
