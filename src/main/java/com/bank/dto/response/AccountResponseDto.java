package com.bank.dto.response;

import com.bank.model.Account;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AccountResponseDto {
    private String id;
    private String accountType;
    private String balanceType;
    private BigDecimal balance;
    private BigDecimal transactionLimit;
    private Date openDate;
    private BigDecimal interestRate;
    private Date maturiryDate;
    private String sourceAccount;
    private Integer savingScheduleDay;


    public static AccountResponseDto build(Account account){
        return builder()
                .id(account.getId())
                .accountType(account.getType().toString())
                .balanceType(account.getBalanceType().toString())
                .balance(account.getBalance())
                .transactionLimit(account.getTransactionLimit())
                .openDate(account.getCreateDate())
                .interestRate(account.getInterestRate())
                .maturiryDate(account.getMaturiryDate())
                .sourceAccount(account.getSourceAccount() != null ? account.getSourceAccount().getId() : null)
                .savingScheduleDay(account.getSavingScheduleDay())
                .build();
    }
}
