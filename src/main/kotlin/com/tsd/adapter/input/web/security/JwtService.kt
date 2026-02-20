package com.tsd.adapter.input.web.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${tsd.security.jwt.secret}") private val secret: String
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(username: String, role: String, participantId: Int?, registrarId: Int?): String {
        return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .apply { participantId?.let { claim("participant_id", it) } }
            .apply { registrarId?.let { claim("registrar_id", it) } }
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // Valid for 24h
            .signWith(key)
            .compact()
    }

    fun extractAllClaims(token: String): Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
}