package com.bank.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomerUpdateRequestDto {
    private String name;

    @Email(message = "Invalid email format")
    private String email;

    @Size(min = 10, max = 12, message = "Citizen ID should in range from 10 to 12 chars")
    private String citizenId;

    @Pattern(regexp = "^(\\+\\d{1,3})?[-.\\s]?(\\d{9,12})$", message = "Invalid phone number format")
    private String phone;

    private String address;

    private String customerType;
}
