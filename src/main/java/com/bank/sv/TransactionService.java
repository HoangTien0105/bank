package com.bank.sv;

import com.bank.dto.PaginDto;
import com.bank.dto.request.MoneyTransferRequestDto;
import com.bank.dto.request.MoneyUpdateRequest;
import com.bank.dto.response.TransactionResponseDto;

public interface TransactionService {
    PaginDto<TransactionResponseDto> getTransactions(PaginDto<TransactionResponseDto> paginDto, String customerId, String role);
    PaginDto<TransactionResponseDto> getTransactionsOrderByDate(PaginDto<TransactionResponseDto> paginDto);
    TransactionResponseDto getTransactionById(String id);
    TransactionResponseDto transferMoney(MoneyTransferRequestDto request, String customerId);
    TransactionResponseDto depositMoney(MoneyUpdateRequest request, String customerId);
    TransactionResponseDto withdrawMoney(MoneyUpdateRequest request, String customerId);
}
