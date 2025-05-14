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

import java.util.Date;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    @Query("SELECT a FROM Account a WHERE a.customer.id = :customerId")
    List<Account> findByCustomerId(@Param("customerId") String customerId);

    @Query("SELECT a FROM Account a ORDER BY a.balanceType, a.balance DESC")
    Page<Account> findAllOrderedByAccountTypeAndBalanceDesc(Pageable pageable);

    List<Account> findByTypeAndStatusAndMaturiryDateLessThanEqual(AccountType type, AccountStatus status, Date date);
}
