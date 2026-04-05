package com.capstone.userauthentication.configs;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    private SecretKey getSecretKey(String keyString){
        return Keys.hmacShaKeyFor(keyString.getBytes(StandardCharsets.UTF_8));
    }

    // ─── Access Token ────────────────────────────────────────────────────────
    /*
     * Generates an access token with role embedded as a claim.
     * The API Gateway reads "role" to forward X-User-Role to downstream services.
     */

    public String generateToken(UserDetails userDetails){
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails){
        Map<String, Object> claims = new HashMap<>(extraClaims);
        // Embed ROLE in token — gateway will forward as X-User-Role header
        if(!claims.containsKey("role")){
            userDetails.getAuthorities().stream()
                    .findFirst()
                    .ifPresent(a -> claims.put("role", a.getAuthority()));
        }
        return buildToken(claims, userDetails, secret, expiration);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername())
                && !isTokenExpired(token, secret);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject, secret);
    }

    public long getExpirationMillis(String token) {
        return extractClaim(token, Claims::getExpiration, secret).getTime()
                - System.currentTimeMillis();
    }

    // ─── Shared Helpers ──────────────────────────────────────────────────────

    private String buildToken(Map<String, Object> claims, UserDetails userDetails,
                              String signingSecret, long ttl) {
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttl))
                .signWith(getSecretKey(signingSecret))
                .compact();
    }

    private boolean isTokenExpired(String token, String signingSecret) {
        return extractClaim(token, Claims::getExpiration, signingSecret).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver, String signingSecret) {
        return resolver.apply(extractAllClaims(token, signingSecret));
    }

    private Claims extractAllClaims(String token, String signingSecret) {
        return Jwts.parser()
                .verifyWith(getSecretKey(signingSecret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
