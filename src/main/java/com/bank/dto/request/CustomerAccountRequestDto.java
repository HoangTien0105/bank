package com.bank.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomerAccountRequestDto {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Citizen ID is required")
    @Size(min = 10, max = 12, message = "Citizen ID should in range from 10 to 12 chars")
    private String citizenId;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^(\\+\\d{1,3})?[-.\\s]?(\\d{9,12})$", message = "Invalid phone number format")
    private String phone;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 chars")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotBlank(message = "Customer type is required")
    private String customerType;
}
