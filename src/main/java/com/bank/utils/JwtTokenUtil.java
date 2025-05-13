package com.bank.utils;

import com.bank.model.CustomerType;
import com.bank.model.JwtUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtTokenUtil {
    @Value("${jwt.secret}") //Lấy value trong application.properties
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    } // Tạo secret key từ thuật toán HMAC

    public String generateToken(UserDetails userDetails){
        Map<String, Object> claims = new HashMap<>();
        String tokenId = UUID.randomUUID().toString();
        claims.put("tokenId", tokenId);
        claims.put("type", "access");

        // Add JwtUser information if available
        if (userDetails instanceof JwtUser) {
            JwtUser jwtUser = (JwtUser) userDetails;
            claims.put("name", jwtUser.getName());
            claims.put("phone", jwtUser.getPhone());
            claims.put("email", jwtUser.getEmail());
            claims.put("citizenId", jwtUser.getCitizenId());
            claims.put("address", jwtUser.getAddress());
            if (jwtUser.getTypeId() != null) {
                claims.put("typeId", jwtUser.getTypeId().toString());
            }
            claims.put("role", jwtUser.getRole());
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String tokenId = UUID.randomUUID().toString();
        claims.put("tokenId", tokenId);
        claims.put("type", "refresh");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 10 * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Boolean validateToken(String token, UserDetails userDetails){
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public String getUsernameFromToken(String token){
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public JwtUser getUserFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Long typeId = claims.get("typeId", Long.class);

        return JwtUser.builder()
                .id(claims.getSubject())
                .name(claims.get("name", String.class))
                .phone(claims.get("phone", String.class))
                .email(claims.get("email", String.class))
                .citizenId(claims.get("citizenId", String.class))
                .address(claims.get("address", String.class))
                .typeId(typeId)
                .role(claims.get("role", String.class))
                .build();
    }
}

