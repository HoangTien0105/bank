package com.bank.repository;

import com.bank.enums.TransactionType;
import com.bank.model.Account;
import com.bank.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    @Query("SELECT t FROM Transaction t ORDER BY t.transactionDate DESC")
    Page<Transaction> findAllTransactionOrderByDate(Pageable pageable);

    List<Transaction> findByAccountAndTypeInOrderByCreateDateAsc(Account account, List<TransactionType> types);

    Transaction findFirstByAccountAndTypeOrderByTransactionDateAsc(Account account, TransactionType type);
    List<Transaction> findByAccountAndTypeOrderByTransactionDateAsc(Account account, TransactionType type);
}
