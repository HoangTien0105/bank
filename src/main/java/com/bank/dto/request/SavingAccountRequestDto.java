package com.bank.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SavingAccountRequestDto {

    @NotNull
    private String sourceAccountId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private Long termMonths;

    @Positive
    private BigDecimal monthlyDepositAmount;
}
