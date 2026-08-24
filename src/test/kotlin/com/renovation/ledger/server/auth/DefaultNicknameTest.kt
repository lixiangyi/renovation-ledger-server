package com.renovation.ledger.server.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DefaultNicknameTest {
    @Test
    fun phoneLastFour() {
        assertEquals("momo-8000", DefaultNickname.fromSuffix("13800138000"))
        assertEquals("momo-8999", DefaultNickname.fromSuffix("13800138999"))
    }

    @Test
    fun openidLastFour() {
        assertEquals("momo-id_1", DefaultNickname.fromSuffix("mp_openid_1"))
        assertEquals("momo-PfL2", DefaultNickname.fromSuffix("oXXXXPfL2"))
    }

    @Test
    fun shorterThanFourUsesAll() {
        assertEquals("momo-ab", DefaultNickname.fromSuffix("ab"))
        assertEquals("momo-", DefaultNickname.fromSuffix("   "))
    }
}
