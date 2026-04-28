package com.bank.ciftpin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret:BankCifTpinSuperSecretKeyForJWTSigningMustBe256BitsLong!}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}") // 24 hours default
    private long jwtExpiration;

    /**
     * Generate a JWT token for an authenticated CIF.
     */
    public String generateToken(String cifNumber) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "CIF_SESSION");
        return buildToken(claims, cifNumber, jwtExpiration);
    }

    /**
     * Extract the CIF number (subject) from a token.
     */
    public String extractCifNumber(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Validate the token against the given CIF number.
     */
    public boolean isTokenValid(String token, String cifNumber) {
        final String extractedCif = extractCifNumber(token);
        boolean valid = extractedCif.equals(cifNumber) && !isTokenExpired(token);
        if (!valid) {
            log.warn("Invalid or expired JWT token for CIF: {}", cifNumber);
        }
        return valid;
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}