package id.ac.ui.cs.advprog.bidmart.backend.auth.security;

import id.ac.ui.cs.advprog.bidmart.backend.auth.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class JwtService {
    private final AuthProperties props;
    private final Key key;

    public JwtService(AuthProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String email) {
        return generateAccessToken(userId, email, null, List.of());
    }

    public String generateAccessToken(UUID userId, String email, UUID sessionId, List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(props.getAccessTokenExpiration());

        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp));

        if (sessionId != null) {
            builder.claim("sid", sessionId.toString());
        }

        return builder
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}