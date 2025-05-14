package com.bank.model;

import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;
import com.bank.enums.BalanceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "account")
public class Account extends BaseEntity{

    @Column(name = "balance_type")
    @Enumerated(EnumType.STRING)
    private BalanceType balanceType;

    @Column(name = "account_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType type;

    @Column(name = "account_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(name = "balance", columnDefinition = "decimal")
    private BigDecimal balance;

    @Column(name = "transaction_limit", columnDefinition = "decimal")
    private BigDecimal transactionLimit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    //Tài khoản tiết kiệm
    //Lãi suất
    @Column(name = "interest_rate", columnDefinition = "decimal")
    private BigDecimal interestRate;

    //Ngày đáo hạn
    @Column(name = "maturity_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date maturiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @Column(name = "saving_schedule_day")
    private Integer savingScheduleDay;
}
