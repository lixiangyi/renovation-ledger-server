package com.renovation.ledger.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LedgerServerApplication

fun main(args: Array<String>) {
    runApplication<LedgerServerApplication>(*args)
}
