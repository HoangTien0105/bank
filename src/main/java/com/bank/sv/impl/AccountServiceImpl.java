package com.bank.sv.impl;

import com.bank.constant.Message;
import com.bank.dto.PaginDto;
import com.bank.dto.request.UpdateAccountStatusRequestDto;
import com.bank.dto.response.AccountResponseDto;
import com.bank.enums.AccountStatus;
import com.bank.model.Account;
import com.bank.model.AccountStatusHistory;
import com.bank.repository.AccountRepository;
import com.bank.repository.AccountStatusHistoryRepository;
import com.bank.sv.AccountService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountStatusHistoryRepository accountStatusHistoryRepository;

    @PersistenceContext
    private EntityManager entityManager;

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

    @Override
    public PaginDto<AccountResponseDto> getAccounts(PaginDto<AccountResponseDto> pagin) {

        int offset = pagin.getOffset() != null ? pagin.getOffset() : 0;
        int limit = pagin.getLimit() != null ? pagin.getLimit() : 10;

        String keyword = pagin.getKeyword();

        int pageNumber = offset / limit;

        String jpql = "SELECT a FROM Account a WHERE " +
                "(:keyword IS NULL OR " +
                "LOWER(a.status) LIKE :searchPattern OR " +
                "LOWER(a.type) LIKE :searchPattern)";

        TypedQuery<Account> query = entityManager.createQuery(jpql, Account.class);

        if (StringUtils.hasText(keyword)) {
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            query.setParameter("keyword", keyword);
            query.setParameter("searchPattern", searchPattern);
        } else {
            query.setParameter("keyword", null);
            query.setParameter("searchPattern", null);
        }

        List<Account> accounts = query
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        String countJpql = "SELECT COUNT(a) FROM Account a WHERE " +
                "(:keyword IS NULL OR " +
                "LOWER(a.status) LIKE :searchPattern OR " +
                "LOWER(a.type) LIKE :searchPattern)";

        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        if (StringUtils.hasText(keyword)) {
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            countQuery.setParameter("keyword", keyword);
            countQuery.setParameter("searchPattern", searchPattern);
        } else {
            countQuery.setParameter("keyword", null);
            countQuery.setParameter("searchPattern", null);
        }

        Long totalRows = countQuery.getSingleResult();

        List<AccountResponseDto> response = accounts.stream()
                .map(AccountResponseDto::build)
                .toList();

        pagin.setResults(response);
        pagin.setOffset(offset);
        pagin.setLimit(limit);
        pagin.setPageNumber(pageNumber + 1);
        pagin.setTotalRows(totalRows);
        pagin.setTotalPages((int) Math.ceil((double) totalRows / limit));

        return pagin;
    }

    @Override
    public AccountResponseDto getAccountById(String id) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new RuntimeException(Message.ACCOUNT_NOT_FOUND));
        return AccountResponseDto.build(account);
    }
}
