package com.bank.sv;

import com.bank.dto.PaginDto;
import com.bank.dto.request.UpdateAccountStatusRequestDto;
import com.bank.dto.response.AccountResponseDto;

public interface AccountService {
    void updateAccountStatus(String id, UpdateAccountStatusRequestDto request);
    PaginDto<AccountResponseDto> getAccounts(PaginDto<AccountResponseDto> pagin);
    AccountResponseDto getAccountById(String id);
    PaginDto<AccountResponseDto> getAccountsGroupByType(PaginDto<AccountResponseDto> paginDto);
}
