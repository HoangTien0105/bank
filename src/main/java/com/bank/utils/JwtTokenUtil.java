package com.bank.utils;

import com.bank.model.Customer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails){
        try{
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String user = claims.getSubject();
            return user.equals(userDetails.getUsername()) && !isTokenExpired(claims);
        } catch (Exception e){
            return false;
        }
    }

    private Boolean isTokenExpired(Claims claims){
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }

    public String getUsernameFromToken(String token){
        try{
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getBody();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}

