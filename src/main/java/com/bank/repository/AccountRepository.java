package com.bank.repository;

import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;
import com.bank.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    @Query("SELECT a FROM Account a WHERE a.customer.id = :customerId")
    List<Account> findByCustomerId(@Param("customerId") String customerId);

    @Query("SELECT a FROM Account a WHERE a.customer.id = :customerId AND a.type = CHECKING")
    Account findCheckingAccountByCustomerId(@Param("customerId") String customerId);

    @Query("SELECT a FROM Account a ORDER BY a.balanceType, a.balance DESC")
    Page<Account> findAllOrderedByAccountTypeAndBalanceDesc(Pageable pageable);

    @Query("SELECT DISTINCT a FROM Account a LEFT JOIN FETCH a.sourceAccount " +
            "WHERE a.type = :type AND a.status = :status " +
            "AND a.maturityDate <= :date")
    List<Account> findByTypeAndStatusAndmaturityDateLessThanEqual(
            @Param("type") AccountType type,
            @Param("status") AccountStatus status,
            @Param("date") Date date
    );

    @Query("SELECT DISTINCT a FROM Account a LEFT JOIN FETCH a.sourceAccount " +
            "WHERE a.type = :type AND a.status = :status " +
            "AND a.savingScheduleDay = :savingScheduleDay " +
            "AND a.monthlyDepositAmount > :monthlyDepositAmount")
    List<Account> findByTypeAndStatusAndSavingScheduleDayAndMonthlyDepositAmountGreaterThan(
            @Param("type") AccountType type,
            @Param("status") AccountStatus status,
            @Param("savingScheduleDay") Integer savingScheduleDay,
            @Param("monthlyDepositAmount") BigDecimal monthlyDepositAmount
    );

    @Query("SELECT COUNT(a) FROM Account a WHERE a.type = SAVING AND a.createDate BETWEEN :startDate AND :endDate")
    Long countAllNewCreatedSavingAccountsByDate(Date startDate, Date endDate);

    @Query("SELECT DISTINCT a FROM Account a WHERE a.id = :accountId AND a.type = CHECKING AND a.customer.id != :excludeCustomerId")
    Optional<Account> findCheckingAccountByIdAndNotCustomerId(@Param("accountId") String accountId, @Param("excludeCustomerId") String excludeCustomerId);
}
