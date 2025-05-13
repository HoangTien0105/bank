package com.bank.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDto {

    @NotBlank(message = "Email or phone is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
