package com.rs.doanmonhoc.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final long ttlSeconds;

    public JwtTokenService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.ttl-seconds:28800}") long ttlSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
    }

    public AuthPrincipal parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token)
                    .getPayload();
            Integer employeeId = claims.get("employeeId", Integer.class);
            Integer departmentId = claims.get("departmentId", Integer.class);

            List<?> rawRoles = claims.get("roles", List.class);
            Set<String> roles = new HashSet<>();
            if (rawRoles != null) {
                for (Object rawRole : rawRoles) {
                    if (rawRole != null) {
                        roles.add(rawRole.toString());
                    }
                }
            }
            return new AuthPrincipal(employeeId, departmentId, roles);
        } catch (JwtException | ClassCastException ex) {
            return null;
        }
    }

    public String generateToken(AuthPrincipal principal) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);
        return Jwts.builder()
                .subject(String.valueOf(principal.employeeId()))
                .claim("employeeId", principal.employeeId())
                .claim("departmentId", principal.departmentId())
                .claim("roles", principal.roles())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }
}
