package com.renovation.ledger.server.error

class ApiException(
    val status: Int,
    val code: String,
    override val message: String,
) : RuntimeException(message)
