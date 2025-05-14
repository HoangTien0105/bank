package com.bank.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestRateRequestDto {
    @NotNull(message = "Term months is required")
    @Min(value = 1, message = "Term months must be greater than 0")
    private Integer termMonths;

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.0", message = "Rate must be greater than 0")
    @DecimalMax(value = "100.0", message = "Rate must be lower than 100")
    private Double rate;

    @NotBlank(message = "Description is required")
    private String description;
}
