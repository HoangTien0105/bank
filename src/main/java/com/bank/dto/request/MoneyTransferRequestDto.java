package com.bank.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyTransferRequestDto {
    @NotBlank(message = "From account's id is required")
    private String fromAccountId;

    @NotBlank(message = "To account's id is required")
    private String toAccountId;

    @NotBlank(message = "Location is required")
    private String location;

    private String description;

    @NotNull(message = "Amount is required")
    @Min(value = 10000, message = "Amount must be at least 10.000đ")
    private Long amount;
}
