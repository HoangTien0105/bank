package com.bank.ctrl;

import com.bank.dto.CustomerDto;
import com.bank.dto.request.LoginRequestDto;
import com.bank.dto.response.TokenResponseDto;
import com.bank.model.Customer;
import com.bank.model.JwtUser;
import com.bank.sv.CustomerService;
import com.bank.sv.TokenService;
import com.bank.utils.APIResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private APIResponse apiResponse;

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequestDto requestDto){
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            requestDto.getUsername(),
                            requestDto.getPassword()
                    )
            );

            //Lấy thông tin user
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            //Tạo token
            TokenResponseDto tokenResponse = tokenService.generateTokens(userDetails);

            return ResponseEntity.ok(apiResponse.response("Login successful", true, tokenResponse));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(apiResponse.response("Invalid username or password", false, null));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Object> getCurrentUser(Authentication authentication){
        JwtUser userDetails = (JwtUser) authentication.getPrincipal();
        // Kiểm tra nếu là ADMIN
        if ("admin".equals(userDetails.getId())) {
            Map<String, String> adminInfo = new HashMap<>();
            adminInfo.put("role", "ADMIN");
            return ResponseEntity.ok(apiResponse.response("Retrieve admin info successful", true, adminInfo));
        }
        CustomerDto customer = customerService.getCustomerById(userDetails.getUsername());
        return ResponseEntity.ok(apiResponse.response("Retrieve customer successful", true, customer));
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(Authentication authentication){
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        tokenService.deleteCustomerById(userDetails.getUsername());
        return ResponseEntity.ok(apiResponse.response("Logout successful", true, null));
    }
}
