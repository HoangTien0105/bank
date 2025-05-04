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

    public static TransactionResponseDto build(Transaction transaction){
        return builder()
                .id(transaction.getId())
                .type(transaction.getType().toString())
                .amount(transaction.getAmount())
                .transactionDate(transaction.getTransactionDate())
                .fee(transaction.getFee())
                .location(transaction.getLocation())
                .description(transaction.getDescription())
                .createDate(transaction.getCreateDate())
                .build();
    }
}
