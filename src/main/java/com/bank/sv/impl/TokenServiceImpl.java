package com.bank.sv.impl;

import com.bank.constant.Message;
import com.bank.dto.response.TokenResponseDto;
import com.bank.model.Customer;
import com.bank.model.JwtUser;
import com.bank.model.Token;
import com.bank.repository.CustomerRepository;
import com.bank.repository.TokenRepository;
import com.bank.sv.TokenService;
import com.bank.utils.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TokenServiceImpl implements TokenService {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

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
        // Tạo access token
        String accessToken = jwtTokenUtil.generateToken(userDetails);

        // Tạo refresh token
        String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        // Nếu không phải admin thì tìm customer
        if (!userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            String customerId = userDetails.getUsername();

            //Xóa token hiện tại trong database
            tokenRepository.deleteByCustomerId(customerId);

            Customer customer = customerRepository.findById(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            Token token = Token.builder()
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

    @Override
    public JwtUser getUserFromToken(String token) {
        return jwtTokenUtil.getUserFromToken(token);
    }

    @Override
    //HttpServletRequest để lấy được Header HTTP
    public String extractRefreshTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    @Transactional
    public TokenResponseDto refreshToken(String refreshToken) {
        try{
            //Verify với parse token.
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();

            //Kiểm tra type của token
            String type = claims.get("type", String.class);
            if(!"refresh".equals(type)){
                throw new RuntimeException("Invalid token type");
            }

            //Kiểm tra xem token đã hết hạn chưa
            if (claims.getExpiration().before(new Date())) {
                throw new RuntimeException("Refresh token has expired");
            }

            //Kiểm tra token có tồn tại trong database
            Token token = tokenRepository.findByRefreshToken(refreshToken);
            if(token == null){
                throw new RuntimeException("Refresh token not found!");
            }

            if (token.getRefreshTokenExpiry().before(new Date())) {
                throw new RuntimeException("Refresh token has expired");
            }

            //Lấy thông tin user từ token
            String user = claims.getSubject();
            Customer customer = customerRepository.findById(user).orElseThrow(() -> new RuntimeException("Customer not found"));

            JwtUser jwtUser = JwtUser.builder()
                    .id(customer.getId())
                    .name(customer.getName())
                    .phone(customer.getPhone())
                    .email(customer.getEmail())
                    .citizenId(customer.getCitizenId())
                    .address(customer.getAddress())
                    .typeId(customer.getType().getId())
                    .role("CUSTOMER")
                    .build();

            //Tạo cặp token mới
            return generateTokens(jwtUser);
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Refresh token has expired", e);
        } catch (Exception e) {
            throw new RuntimeException("Error processing refresh token", e);
        }
    }
}
