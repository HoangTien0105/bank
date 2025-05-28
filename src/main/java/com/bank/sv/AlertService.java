package com.bank.sv;

import com.bank.dto.PaginDto;
import com.bank.dto.response.AlertResponseDto;
import com.bank.model.Transaction;

public interface AlertService {
    void detectAbnormalTransactions();
    PaginDto<AlertResponseDto> getAlerts(PaginDto<AlertResponseDto> paginDto, String role);
    AlertResponseDto getAlertById(String id);
    void updateAlertStatus(String id, String status, String processedBy, String notes);
    void isTransactionAbnormal(Transaction transaction);
    void checkAndCreateAlert(Transaction transaction);
}
