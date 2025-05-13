package com.bank.sv;

import com.bank.dto.response.TokenResponseDto;
import org.springframework.security.core.userdetails.UserDetails;

public interface TokenService {
    String generateAccessToken(UserDetails userDetails);
    String generateRefreshToken(UserDetails userDetails);
    boolean validateToken(String token, UserDetails userDetails);
    String getUsernameFromToken(String token);
    boolean isTokenExpired(String token);
    TokenResponseDto generateTokens(UserDetails userDetails);
    void deleteCustomerById(String id);
}
