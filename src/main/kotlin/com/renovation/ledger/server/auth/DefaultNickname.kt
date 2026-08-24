package com.renovation.ledger.server.auth

object DefaultNickname {
    fun fromSuffix(raw: String): String = "momo-" + raw.trim().takeLast(4)
}
