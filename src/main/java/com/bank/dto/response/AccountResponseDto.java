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
    private BigDecimal balance;
    private BigDecimal transactionLimit;
    private Date openDate;

    public static AccountResponseDto build(Account account){
        return builder()
                .id(account.getId())
                .accountType(account.getType().toString())
                .balance(account.getBalance())
                .transactionLimit(account.getTransactionLimit())
                .openDate(account.getCreateDate())
                .build();
    }
}
