package com.bank.sv;

import com.bank.dto.PaginDto;
import com.bank.dto.request.MoneyTransferRequestDto;
import com.bank.dto.response.TransactionResponseDto;

public interface TransactionService {
    PaginDto<TransactionResponseDto> getTransactions(PaginDto<TransactionResponseDto> pagin);
    TransactionResponseDto getTransactionById(String id);
    void transferMoney(MoneyTransferRequestDto request);
}
