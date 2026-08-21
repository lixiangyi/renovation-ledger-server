package com.renovation.ledger.server.auth

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class JwtFilterTest {
    @Autowired lateinit var mockMvc: MockMvc

    @Test
    fun ledgersWithoutTokenIs401() {
        mockMvc.get("/ledgers").andExpect { status { isUnauthorized() } }
    }
}
