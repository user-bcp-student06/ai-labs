package com.bcp.workshop.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@ApplicationScoped
public class TokenSigner {
    public static final long TOKEN_TTL_SECONDS = 3600;
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLE = "role";

    private final SecretKey key;

    @Inject
    public TokenSigner(@ConfigProperty(name = "workshop.auth.token-secret") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(String username, String role) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(TOKEN_TTL_SECONDS)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
