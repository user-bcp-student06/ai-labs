package com.bcp.workshop.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenSignerTest {
    private static final String SECRET = "unit-test-secret-not-for-production-0123456789";
    private static final String OTHER_SECRET = "other-unit-test-secret-not-for-production-4567";

    @Test
    void issuesSignedTokenWithExplicitClaimsAndExpiry() {
        String token = new TokenSigner(SECRET).issue("student", "STUDENT");

        Claims claims = parse(token, SECRET);

        assertEquals("student", claims.get(TokenSigner.CLAIM_USERNAME));
        assertEquals("STUDENT", claims.get(TokenSigner.CLAIM_ROLE));
        assertEquals(TokenSigner.TOKEN_TTL_SECONDS,
                (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000);
    }

    @Test
    void tokenIsNotBase64OfPlainTextWithEmbeddedSecret() {
        String token = new TokenSigner(SECRET).issue("student", "STUDENT");

        assertFalse(token.contains("|"));
        assertFalse(new String(Base64.getDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8).contains(SECRET));
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new TokenSigner(OTHER_SECRET).issue("student", "STUDENT");

        assertThrows(SignatureException.class, () -> parse(token, SECRET));
    }

    @Test
    void tamperedPayloadIsRejected() {
        String token = new TokenSigner(SECRET).issue("student", "STUDENT");
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"username\":\"trainer\",\"role\":\"TRAINER\",\"exp\":4102444800}".getBytes(StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThrows(SignatureException.class, () -> parse(forged, SECRET));
    }

    @Test
    void expiredTokenIsRejected() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder()
                .claim(TokenSigner.CLAIM_USERNAME, "student")
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> parse(expired, SECRET));
    }

    @Test
    void shortSecretIsRejectedAtConstruction() {
        assertThrows(RuntimeException.class, () -> new TokenSigner("too-short"));
    }

    @Test
    void issuedTokenIsValidNow() {
        String token = new TokenSigner(SECRET).issue("student", "STUDENT");

        assertTrue(parse(token, SECRET).getExpiration().after(new Date()));
    }

    private static Claims parse(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
