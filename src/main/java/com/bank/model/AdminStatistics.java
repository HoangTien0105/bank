package com.bank.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "admin_stats")
public class AdminStatistics{

    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statistics_date", nullable = false)
    private LocalDateTime date;

    @Column(name = "total_transactions")
    private Long totalTransactions;

    @Column(name = "max_transaction_amount", columnDefinition = "decimal")
    private BigDecimal maxTransactionAmount;

    @Column(name = "avg_transaction_amount", columnDefinition = "decimal")
    private BigDecimal avgTransactionAmount;

    @Column(name = "min_transaction_amount", columnDefinition = "decimal")
    private BigDecimal minTransactionAmount;

    @Column(name = "new_customers")
    private Long newCustomers;

    @Column(name = "total_customers")
    private Long totalCustomers;

    @Column(name = "new_saving_accounts")
    private Long newSavingAccounts;
}
