package com.bank.sv;

import com.bank.dto.PaginDto;
import com.bank.dto.request.SavingAccountRequestDto;
import com.bank.dto.request.UpdateAccountStatusRequestDto;
import com.bank.dto.response.AccountResponseDto;

public interface AccountService {
    void updateAccountStatus(String id, UpdateAccountStatusRequestDto request);
    PaginDto<AccountResponseDto> getAccounts(PaginDto<AccountResponseDto> paginDto, String customerId, String role);
    AccountResponseDto getAccountById(String id);
    AccountResponseDto getTrackingAccountByCusId(String cusId);
    AccountResponseDto createSavingAccount(SavingAccountRequestDto requestDto, String customerId);
    void processSavingAccount();
    void monthlyDeposit();
}
