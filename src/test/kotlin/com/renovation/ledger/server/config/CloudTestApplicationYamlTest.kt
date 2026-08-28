package com.renovation.ledger.server.config

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class CloudTestApplicationYamlTest {
    @Test
    fun cloudTestProfileReturnsSmsCodeInResponse() {
        val file = File("src/main/resources/application-cloud-test.yml")
        assertTrue(file.isFile, "missing application-cloud-test.yml")
        val yaml = file.readText()
        assertTrue(yaml.contains("return-code-in-response: true"))
    }
}
