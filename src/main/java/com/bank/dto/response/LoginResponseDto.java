package com.bank.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {
    private String token;
    private String email;
    private String phone;
    private String role;
    private String address;
}
