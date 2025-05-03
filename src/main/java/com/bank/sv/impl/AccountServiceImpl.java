package com.bank.sv.impl;

import com.bank.constant.Message;
import com.bank.dto.request.UpdateAccountStatusRequestDto;
import com.bank.enums.AccountStatus;
import com.bank.model.Account;
import com.bank.model.AccountStatusHistory;
import com.bank.repository.AccountRepository;
import com.bank.repository.AccountStatusHistoryRepository;
import com.bank.sv.AccountService;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountStatusHistoryRepository accountStatusHistoryRepository;

    //Status cho phép thay đổi
    private static final Map<AccountStatus, Set<AccountStatus>> allowStatusTransaction = Map.of(
            AccountStatus.ACTIVE, Set.of(AccountStatus.INACTIVE, AccountStatus.SUSPENDED),
            AccountStatus.INACTIVE, Set.of(AccountStatus.ACTIVE),
            AccountStatus.SUSPENDED, Set.of(AccountStatus.ACTIVE, AccountStatus.CLOSED)
    );

    @Override
    @Transactional
    public void updateAccountStatus(String id, UpdateAccountStatusRequestDto request) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new RuntimeException(Message.ACCOUNT_NOT_FOUND));

        // Validate và chuyển đổi status từ String sang AccountStatus
        AccountStatus newStatus;
        try{
            newStatus = AccountStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(Message.INVALID_ACCOUNT_STATUS);
        }

        if(account.getStatus() == newStatus){
            throw new ValidationException(Message.ACCOUNT_STATUS_ALREADY_SET);
        }

        account.setStatus(newStatus);
        accountRepository.save(account);

        AccountStatusHistory history = AccountStatusHistory.builder()
                .status(account.getStatus())
                .reason(request.getReason())
                .customer(account.getCustomer())
                .build();

        accountStatusHistoryRepository.save(history);
    }
}
