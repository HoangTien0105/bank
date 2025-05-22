package com.bank.dto.response;

import com.bank.model.CustomerStatistics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomerStatsResponseDto {
    private Long id;
    private Integer year;
    private Integer month;
    private String customer;
    private Long totalTransactions;
    private BigDecimal maxTransactionAmount;
    private BigDecimal avgTransactionAmount;
    private BigDecimal minTransactionAmount;
    private BigDecimal endMonthBalance;
    private BigDecimal totalTransactionAmount;

    public static CustomerStatsResponseDto build(CustomerStatistics customerStatistics){
        return builder()
                .id(customerStatistics.getId())
                .year(customerStatistics.getYear())
                .month(customerStatistics.getMonth())
                .customer(customerStatistics.getCustomer().getId())
                .totalTransactions(customerStatistics.getTotalTransactions())
                .totalTransactionAmount(customerStatistics.getTotalTransactionAmount())
                .maxTransactionAmount(customerStatistics.getMaxTransactionAmount())
                .avgTransactionAmount(customerStatistics.getAvgTransactionAmount())
                .minTransactionAmount(customerStatistics.getMinTransactionAmount())
                .endMonthBalance(customerStatistics.getEndMonthBalance())
                .totalTransactionAmount(customerStatistics.getTotalTransactionAmount())
                .build();
    }
}
