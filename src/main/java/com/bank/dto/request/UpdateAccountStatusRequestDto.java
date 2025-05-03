package com.bank.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountStatusRequestDto {
    @NotBlank(message = "Status is required")
    private String status;

    @NotBlank(message = "Reason is required")
    private String reason;
}
