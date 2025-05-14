package com.bank;

import com.bank.model.JwtUser;
import com.bank.sv.TokenService;
import com.bank.sv.impl.CustomUserDetailsService;
import com.bank.utils.JwtTokenUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    @Autowired
    private TokenService tokenService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try{
            String authToken = request.getHeader("Authorization");
            if(authToken != null && authToken.startsWith("Bearer ")){
                authToken = authToken.substring(7);
                // Lấy customer id từ token
                String username = jwtTokenUtil.getUsernameFromToken(authToken);
                // Nếu cus id k null và chưa có tài khoản nào đã được xác thực trong context
                if(username != null && SecurityContextHolder.getContext().getAuthentication() == null){
                    // Lấy thông tin chi tiết từ token thay vì database để cải thiện hiệu suất
                    JwtUser jwtUser = tokenService.getUserFromToken(authToken);
                    if(jwtTokenUtil.validateToken(authToken, jwtUser)){
                        // Tạo object xác thực với thông tin đầy đủ từ token
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(jwtUser, null, jwtUser.getAuthorities());

                        //Thiết lập chi tiết yêu cầu
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        //Lưu xác thực vào SecurityContext
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Access token đã hết hạn\",\"success\":false}");
            return;
        } catch (Exception e){
            logger.error("Không thể set user authentication: {}", e);
        }
        //Chuyển yêu cầu đến bộ lọc tiếp theo
        filterChain.doFilter(request, response);
    }
}
