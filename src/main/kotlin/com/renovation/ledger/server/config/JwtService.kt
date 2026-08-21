package com.renovation.ledger.server.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date

@Service
class JwtService(
    @Value("\${app.jwt-secret}") secret: String,
    @Value("\${app.jwt-days:30}") private val jwtDays: Long,
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8).let { bytes ->
        if (bytes.size >= 32) bytes else bytes.copyOf(32)
    })

    fun create(userId: String): String {
        val now = Instant.now()
        val exp = now.plusSeconds(jwtDays * 24 * 3600)
        return Jwts.builder()
            .subject(userId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(key)
            .compact()
    }

    fun parseUserId(token: String): String =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.subject
}
