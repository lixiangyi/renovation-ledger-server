package com.renovation.ledger.server.auth

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class SmsCodeStore {
    data class Entry(val code: String, val expiresAtMs: Long)

    private val map = ConcurrentHashMap<String, Entry>()

    fun put(phone: String, code: String, ttlSeconds: Long) {
        map[phone] = Entry(code, System.currentTimeMillis() + ttlSeconds * 1000)
    }

    fun consume(phone: String, code: String): Boolean {
        val entry = map[phone] ?: return false
        if (entry.expiresAtMs < System.currentTimeMillis()) {
            map.remove(phone)
            return false
        }
        if (entry.code != code) return false
        map.remove(phone)
        return true
    }
}
