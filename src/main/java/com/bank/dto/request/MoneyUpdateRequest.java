package com.bank.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MoneyUpdateRequest {
    @NotBlank(message = "Account's id is required")
    private String accountId;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Amount is required")
    @Min(value = 5000, message = "Amount must be at least 5.000đ")
    private Long amount;
}
