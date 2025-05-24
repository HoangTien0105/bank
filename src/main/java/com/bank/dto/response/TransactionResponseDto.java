package com.bank.dto.response;

import com.bank.model.Transaction;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TransactionResponseDto {
    private String id;
    private String type;
    private BigDecimal amount;
    private Date transactionDate;
    private BigDecimal fee;
    private String location;
    private String description;
    private Date createDate;
    private String accountId;
    private String fromAccountId;
    private String toAccountId;

    public static TransactionResponseDto build(Transaction transaction) {
        return builder()
                .id(transaction.getId())
                .type(transaction.getType().toString())
                .amount(transaction.getAmount())
                .transactionDate(transaction.getTransactionDate())
                .location(transaction.getLocation())
                .description(transaction.getDescription())
                .createDate(transaction.getCreateDate())
                .accountId(transaction.getAccount() != null ? transaction.getAccount().getId() : null)
                .fromAccountId(transaction.getFromAccountId())
                .toAccountId(transaction.getToAccountId())
                .build();
    }
}