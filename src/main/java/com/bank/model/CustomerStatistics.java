package com.bank.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "customer_stats")
public class CustomerStatistics {

    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stats_year", nullable = false)
    private Integer year;

    @Column(name = "stats_month", nullable = false)
    private Integer month;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "total_transactions")
    private Long totalTransactions;

    @Column(name = "max_transaction_amount", columnDefinition = "decimal")
    private BigDecimal maxTransactionAmount;

    @Column(name = "avg_transaction_amount", columnDefinition = "decimal")
    private BigDecimal avgTransactionAmount;

    @Column(name = "min_transaction_amount", columnDefinition = "decimal")
    private BigDecimal minTransactionAmount;

    @Column(name = "end_month_balance", columnDefinition = "decimal")
    private BigDecimal endMonthBalance;

    @Column(name = "total_transaction_amount", columnDefinition = "decimal")
    private BigDecimal totalTransactionAmount;
}