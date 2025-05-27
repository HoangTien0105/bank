package com.bank.repository;

import com.bank.enums.TransactionType;
import com.bank.model.Account;
import com.bank.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    @Query("SELECT t FROM Transaction t ORDER BY t.transactionDate DESC")
    Page<Transaction> findAllTransactionOrderByDate(Pageable pageable);

    Transaction findFirstByAccountAndTypeOrderByTransactionDateAsc(Account account, TransactionType type);

    List<Transaction> findByAccountAndTypeOrderByTransactionDateAsc(Account account, TransactionType type);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    Long countAllTransactionByDate(Date startDate, Date endDate);

    @Query("SELECT COUNT(t) FROM Transaction t")
    Long countAllTransactions();

    @Query("SELECT MAX(t.amount) FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getHighestAmountOfTransferByDate(Date startDate, Date endDate);

    @Query("SELECT AVG(t.amount) FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getAvgAmountOfTransferByDate(Date startDate, Date endDate);

    @Query("SELECT MIN(t.amount) FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getMinAmountOfTransferByDate(Date startDate, Date endDate);

    @Query("SELECT t FROM Transaction t " +
            "JOIN FETCH t.account a " +
            "JOIN FETCH a.customer c " +
            "JOIN FETCH c.type " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findAllByTransactionDateBetweenWithCustomerType(
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    List<Transaction> findByAccountAndTransactionDateBetween(Account account, Date startDate, Date endDate);
}
