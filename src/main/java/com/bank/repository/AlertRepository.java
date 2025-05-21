package com.bank.repository;

import com.bank.enums.AlertStatus;
import com.bank.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {
    List<Alert> findByStatus(AlertStatus status);
    List<Alert> findByTransactionAccountId(String accountId);
    boolean existsByTransactionId(String transactionId);
    List<Alert> findByCreateDateBetween(Date startDate, Date endDate);
}
