package com.bank.sv.impl;

import com.bank.constant.Message;
import com.bank.dto.response.TokenResponseDto;
import com.bank.model.Customer;
import com.bank.model.Token;
import com.bank.repository.CustomerRepository;
import com.bank.repository.TokenRepository;
import com.bank.sv.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TokenServiceImpl implements TokenService {
    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refreshExpiration}")
    private Long refreshExpiration;

    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String tokenId = UUID.randomUUID().toString();
        claims.put("tokenId", tokenId);
        claims.put("type", "access");

        String accessToken = Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        Customer customer = customerRepository.findById(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("Customer not found"));;

        Token token = Token.builder()
                .accessToken(accessToken)
                .accessTokenExpiry(new Date(System.currentTimeMillis() + expiration * 1000))
                .customer(customer)
                .build();

        tokenRepository.save(token);

        return accessToken;
    }

    @Override
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String tokenId = UUID.randomUUID().toString();
        claims.put("tokenId", tokenId);
        claims.put("type", "refresh");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public boolean validateToken(String token, UserDetails userDetails) {
        try{
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e){
            return false;
        }
    }

    @Override
    public String getUsernameFromToken(String token) {
        try{
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isTokenExpired(String token) {
        try{
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public TokenResponseDto generateTokens(UserDetails userDetails) {
        Map<String, Object> accessClaims = new HashMap<>();
        String accessTokenId = UUID.randomUUID().toString();
        accessClaims.put("tokenId", accessTokenId);
        accessClaims.put("type", "access");

        String accessToken = Jwts.builder()
                .setClaims(accessClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        // Tạo refresh token
        Map<String, Object> refreshClaims = new HashMap<>();
        String refreshTokenId = UUID.randomUUID().toString();
        refreshClaims.put("tokenId", refreshTokenId);
        refreshClaims.put("type", "refresh");

        String refreshToken = Jwts.builder()
                .setClaims(refreshClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        Token token = null;

        //Nếu không phải admin thì mới tìm customer
        if (!userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            String customerId = userDetails.getUsername();

            tokenRepository.deleteByCustomerId(customerId);

            Customer customer = customerRepository.findById(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            token = Token.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .accessTokenExpiry(new Date(System.currentTimeMillis() + expiration * 1000))
                    .refreshTokenExpiry(new Date(System.currentTimeMillis() + refreshExpiration * 1000))
                    .customer(customer)
                    .build();

            tokenRepository.save(token);
        }

        // Trả về response
        return TokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiration)
                .build();
    }

    @Override
    @Transactional
    public void deleteCustomerById(String id) {
        tokenRepository.deleteByCustomerId(id);
    }
}
