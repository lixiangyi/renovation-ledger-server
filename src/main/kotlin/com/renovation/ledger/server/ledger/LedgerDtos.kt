package com.renovation.ledger.server.ledger

data class TaxonomyDto(
    val stages: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val spaces: List<String> = emptyList(),
    val iconsJson: String = "{}",
)

data class PaymentDto(
    val id: String,
    val type: String,
    val amount: Long,
    val status: String,
    val paidAtEpochMs: Long? = null,
    val paidOnDate: String? = null,
    val note: String = "",
    val receiptUri: String? = null,
    val createdByUserId: String? = null,
    val createdByName: String = "",
)

data class ItemDto(
    val id: String,
    val name: String,
    val stage: String,
    val category: String,
    val space: String,
    val budgetAmount: Long,
    val contractAmount: Long? = null,
    val merchant: String = "",
    val recordedDate: String? = null,
    val remark: String = "",
    val isNewAddition: Boolean = false,
    val settledOnDate: String? = null,
    val settledAtEpochMs: Long? = null,
    val payments: List<PaymentDto> = emptyList(),
)

data class CreateLedgerRequest(
    val name: String,
    val localId: String,
)

data class RenameLedgerRequest(
    val name: String,
)

data class ImportLedgerRequest(
    val localId: String,
    val name: String,
    val items: List<ItemDto> = emptyList(),
    val taxonomy: TaxonomyDto = TaxonomyDto(),
)

data class LedgerSummaryDto(
    val id: String,
    val name: String,
    val role: String,
    val revision: Long,
    val createdAtEpochMs: Long = 0,
)

data class LedgerSnapshot(
    val id: String,
    val name: String,
    val revision: Long,
    val items: List<ItemDto>,
    val taxonomy: TaxonomyDto,
)

data class PutItemRequest(
    val baseRevision: Long,
    val item: ItemDto,
)

data class MemberDto(
    val userId: String,
    val nickname: String,
    val role: String,
)

data class InviteCreatedDto(
    val id: String,
    val code: String,
)

data class InvitePreviewDto(
    val code: String,
    val ledgerId: String,
    val ledgerName: String,
    val ownerNickname: String,
    val alreadyMember: Boolean,
)

data class JoinInviteRequest(
    val code: String,
)

data class TransferRequest(
    val userId: String,
)
