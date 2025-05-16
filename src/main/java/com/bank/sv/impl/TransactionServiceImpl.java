package com.bank.sv.impl;

import com.bank.constant.Message;
import com.bank.dto.PaginDto;
import com.bank.dto.request.MoneyTransferRequestDto;
import com.bank.dto.request.MoneyUpdateRequest;
import com.bank.dto.response.AccountResponseDto;
import com.bank.dto.response.TransactionResponseDto;
import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;
import com.bank.enums.BalanceType;
import com.bank.enums.TransactionType;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.sv.TransactionService;
import com.bank.utils.BalanceTypeUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Cacheable(value = "transactions",key = "#paginDto.toString() + '_' + #customerId + '_' + #role")
    public PaginDto<TransactionResponseDto> getTransactions(PaginDto<TransactionResponseDto> paginDto, String customerId, String role) {

        int offset = paginDto.getOffset() != null ? paginDto.getOffset() : 0;
        int limit = paginDto.getLimit() != null ? paginDto.getLimit() : 10;

        String keyword = paginDto.getKeyword();

        int pageNumber = offset / limit;

        String jpql;
        TypedQuery<Transaction> query;

        if ("ADMIN".equals(role)) {
            // Admin can see all transactions
            jpql = "SELECT t FROM Transaction t WHERE " +
                    "(:keyword IS NULL OR " +
                    "LOWER(t.description) LIKE :searchPattern OR " +
                    "LOWER(t.type) LIKE :searchPattern OR " +
                    "LOWER(t.location) LIKE :searchPattern)";

            query = entityManager.createQuery(jpql, Transaction.class);
        } else {
            // Regular customers can only see their own transactions
            jpql = "SELECT t FROM Transaction t WHERE t.account IN " +
                    "(SELECT a FROM Account a WHERE a.customer.id = :customerId) AND " +
                    "(:keyword IS NULL OR " +
                    "LOWER(t.description) LIKE :searchPattern OR " +
                    "LOWER(t.type) LIKE :searchPattern OR " +
                    "LOWER(t.location) LIKE :searchPattern)";

            query = entityManager.createQuery(jpql, Transaction.class);
            query.setParameter("customerId", customerId);
        }

        if (StringUtils.hasText(keyword)) {
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            query.setParameter("keyword", keyword);
            query.setParameter("searchPattern", searchPattern);
        } else {
            query.setParameter("keyword", null);
            query.setParameter("searchPattern", null);
        }

        // Set pagination
        query.setFirstResult(pageNumber * limit);
        query.setMaxResults(limit);

        List<Transaction> transactions = query.getResultList();

        String countJpql = jpql.replace("SELECT t", "SELECT COUNT(t)");

        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
        if ("ADMIN".equals(role)) {
            if (StringUtils.hasText(paginDto.getKeyword())) {
                countQuery.setParameter("keyword", paginDto.getKeyword());
                countQuery.setParameter("searchPattern", "%" + paginDto.getKeyword().toLowerCase() + "%");
            } else {
                countQuery.setParameter("keyword", null);
                countQuery.setParameter("searchPattern", null);
            }
        } else {
            countQuery.setParameter("customerId", customerId);
            if (StringUtils.hasText(paginDto.getKeyword())) {
                countQuery.setParameter("keyword", paginDto.getKeyword());
                countQuery.setParameter("searchPattern", "%" + paginDto.getKeyword().toLowerCase() + "%");
            } else {
                countQuery.setParameter("keyword", null);
                countQuery.setParameter("searchPattern", null);
            }
        }

        Long totalRows = countQuery.getSingleResult();

        List<TransactionResponseDto> response = transactions.stream()
                .map(TransactionResponseDto::build)
                .collect(Collectors.toList());

        paginDto.setResults(response);
        paginDto.setOffset(offset);
        paginDto.setLimit(limit);
        paginDto.setPageNumber(pageNumber + 1);
        paginDto.setTotalRows(totalRows);
        paginDto.setTotalPages((int) Math.ceil((double) totalRows / limit));

        return paginDto ;
    }

    @Override
    public PaginDto<TransactionResponseDto> getTransactionsOrderByDate(PaginDto<TransactionResponseDto> paginDto) {
        int offset = paginDto.getOffset() != null ? paginDto.getOffset() : 0;
        int limit = paginDto.getLimit() != null ? paginDto.getLimit() : 10;

        int pageNumber = offset / limit;

        Pageable pageable = PageRequest.of(pageNumber, limit);

        Page<Transaction> transactions = transactionRepository.findAllTransactionOrderByDate(pageable);

        List<TransactionResponseDto> result = transactions.getContent().stream()
                .map(TransactionResponseDto::build)
                .collect(Collectors.toList());

        PaginDto<TransactionResponseDto> response = new PaginDto<>();
        response.setResults(result);
        response.setLimit(limit);
        response.setOffset(offset);
        response.setTotalPages(transactions.getTotalPages());
        response.setTotalRows(transactions.getTotalElements());

        return response;
    }

    @Override
    public TransactionResponseDto getTransactionById(String id) {
        Transaction transaction = transactionRepository.findById(id).orElseThrow(() -> new RuntimeException(Message.TRANSACTION_NOT_FOUND));
        return TransactionResponseDto.build(transaction);
    }

    @Override
    @Transactional
    public void transferMoney(MoneyTransferRequestDto request, String customerId) {
        String fromAccountId = request.getFromAccountId();
        String toAccountId = request.getToAccountId();
        String description = request.getDescription();

        if(fromAccountId.equals(toAccountId)){
            throw new RuntimeException("Account can't be the same");
        }

        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new RuntimeException("From account does not exists"));
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new RuntimeException("To account does not exists"));

        if(!fromAccount.getCustomer().getId().equals(customerId)){
            throw new RuntimeException("This account does not belong to this customers");
        }

        if(fromAccount.getStatus() != AccountStatus.ACTIVE){
            throw new RuntimeException("Source account must be active");
        }

        if(toAccount.getStatus() != AccountStatus.ACTIVE){
            throw new RuntimeException("Destination account must be active");
        }


        if (fromAccount.getType() != AccountType.CHECKING) {
            throw new RuntimeException("Source account must be a CHECKING account");
        }
        if (toAccount.getType() != AccountType.CHECKING) {
            throw new RuntimeException("Destination account must be a CHECKING account");
        }

        //Convert về big decimal
        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Your account balance is not enough");
        }

        if (fromAccount.getTransactionLimit() != null &&
                fromAccount.getTransactionLimit().compareTo(amount) < 0) {
            throw new RuntimeException("The amount exceeds the transaction limit of the account.");
        }

        //Update balance
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        // Update status and transaction limit
        fromAccount.setBalanceType(BalanceType.valueOf(BalanceTypeUtils.validateBalanceStatus(fromAccount)));
        BalanceTypeUtils.setTransactionLimitBasedOnBalance(fromAccount);

        toAccount.setBalanceType(BalanceType.valueOf(BalanceTypeUtils.validateBalanceStatus(toAccount)));
        BalanceTypeUtils.setTransactionLimitBasedOnBalance(toAccount);

        if (description == null) {
            description = "Transfer from " + fromAccountId + " to " + toAccountId;
        }

        Date transactionDate = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());

        Transaction fromTransaction = new Transaction();
        fromTransaction.setAccount(fromAccount);
        fromTransaction.setAmount(amount);
        fromTransaction.setDescription(description);
        fromTransaction.setLocation(request.getLocation());
        fromTransaction.setType(TransactionType.TRANSFER_OUT);
        fromTransaction.setTransactionDate(transactionDate);

        Transaction toTransaction = new Transaction();
        toTransaction.setAccount(toAccount);
        toTransaction.setAmount(amount);
        toTransaction.setDescription(description);
        toTransaction.setLocation(request.getLocation());
        toTransaction.setType(TransactionType.TRANSFER_IN);
        toTransaction.setTransactionDate(transactionDate);

        transactionRepository.save(fromTransaction);
        transactionRepository.save(toTransaction);

        // Save accounts
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }

    @Override
    @Transactional
    public void depositMoney(MoneyUpdateRequest request, String customerId) {
        Account account = accountRepository.findById(request.getAccountId()).orElseThrow(() -> new RuntimeException("Account not found"));

        if(!account.getCustomer().getId().equals(customerId)){
            throw new RuntimeException("This account does not belong to this customers");
        }

        if(account.getStatus() != AccountStatus.ACTIVE){
            throw new RuntimeException("Account must be active");
        }

        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        account.setBalance(account.getBalance().add(amount));

        account.setBalanceType(BalanceType.valueOf(BalanceTypeUtils.validateBalanceStatus(account)));
        BalanceTypeUtils.setTransactionLimitBasedOnBalance(account);

        Date transactionDate = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionDate(transactionDate);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setLocation(request.getLocation());
        transaction.setDescription("Deposit money into wallet");
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void withdrawMoney(MoneyUpdateRequest request, String customerId) {
        Account account = accountRepository.findById(request.getAccountId()).orElseThrow(() -> new RuntimeException("Account not found"));

        if(!account.getCustomer().getId().equals(customerId)){
            throw new RuntimeException("This account does not belong to this customers");
        }

        if(account.getStatus() != AccountStatus.ACTIVE){
            throw new RuntimeException("Account must be active");
        }

        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Your account balance is not enough");
        }

        account.setBalance(account.getBalance().subtract(amount));

        account.setBalanceType(BalanceType.valueOf(BalanceTypeUtils.validateBalanceStatus(account)));
        BalanceTypeUtils.setTransactionLimitBasedOnBalance(account);

        Date transactionDate = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionDate(transactionDate);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.WITHDRAW);
        transaction.setLocation(request.getLocation());
        transaction.setDescription("Withdraw money from wallet");
        transactionRepository.save(transaction);
    }
}
