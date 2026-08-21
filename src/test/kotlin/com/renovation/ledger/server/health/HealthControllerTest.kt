package com.renovation.ledger.server.health

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {
    @Autowired lateinit var mockMvc: MockMvc

    @Test
    fun healthOkWithoutAuth() {
        mockMvc.get("/health").andExpect {
            status { isOk() }
            jsonPath("$.ok") { value(true) }
        }
    }
}
