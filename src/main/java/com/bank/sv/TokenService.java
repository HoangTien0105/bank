package com.bank.sv;

import com.bank.dto.response.TokenResponseDto;
import com.bank.model.JwtUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;

public interface TokenService {
    boolean validateToken(String token, UserDetails userDetails);
    String getUsernameFromToken(String token);
    boolean isTokenExpired(String token);
    TokenResponseDto generateTokens(UserDetails userDetails);
    void deleteCustomerById(String id);
    JwtUser getUserFromToken(String token);
    String extractRefreshTokenFromRequest(HttpServletRequest request);
    TokenResponseDto refreshToken(String refreshToken);
}
