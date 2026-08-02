package com.shopnest.authservice.security;

import com.shopnest.authservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // ===== BIKIN token (sign) =====
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
        // userId ikut dibawa token - gateway membacanya lalu meneruskan
        // ke service sebagai header X-User-Id
        if (userDetails instanceof User user) {
            claims.put("userId", user.getId().toString());
        }

        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    // ===== CEK token (verify) =====
    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = parseClaims(token);
        boolean notExpired = claims.getExpiration().after(new Date());
        return claims.getSubject().equals(userDetails.getUsername()) && notExpired;
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    // Buka token jadi isinya (claims). Signature diverifikasi di sini -
    // token palsu/kadaluarsa akan melempar exception.
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
