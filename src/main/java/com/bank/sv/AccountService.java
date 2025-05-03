package com.bank.sv;

import com.bank.dto.request.UpdateAccountStatusRequestDto;

public interface AccountService {
    void updateAccountStatus(String id, UpdateAccountStatusRequestDto request);
}
