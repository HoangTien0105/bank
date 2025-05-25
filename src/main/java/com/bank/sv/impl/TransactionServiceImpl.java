package com.bank.sv.impl;

import com.bank.constant.Message;
import com.bank.dto.PaginDto;
import com.bank.dto.request.MoneyTransferRequestDto;
import com.bank.dto.request.MoneyUpdateRequest;
import com.bank.dto.response.TransactionResponseDto;
import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;
import com.bank.enums.BalanceType;
import com.bank.enums.TransactionType;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.sv.AlertService;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AlertService alertService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Cacheable(value = "transactions", key = "#paginDto.toString() + '_' + #customerId + '_' + #role")
    public PaginDto<TransactionResponseDto> getTransactions(PaginDto<TransactionResponseDto> paginDto, String customerId, String role) {
        int offset = paginDto.getOffset() != null ? paginDto.getOffset() : 0;
        int limit = paginDto.getLimit() != null ? paginDto.getLimit() : 10;
        String keyword = paginDto.getKeyword();
        Map<String, Object> options = paginDto.getOptions();
        int pageNumber = offset / limit;

        // Build main query
        StringBuilder jpqlBuilder = new StringBuilder("SELECT t FROM Transaction t");

        if (!"ADMIN".equals(role)) {
            jpqlBuilder.append(" WHERE t.account IN (SELECT a FROM Account a WHERE a.customer.id = :customerId) AND ");
        } else {
            jpqlBuilder.append(" WHERE ");
        }

        jpqlBuilder.append("(:keyword IS NULL OR ")
                .append("t.id LIKE :searchPattern OR ")
                .append("LOWER(t.description) LIKE :searchPattern OR ")
                .append("LOWER(t.type) LIKE :searchPattern OR ")
                .append("LOWER(t.location) LIKE :searchPattern)");

        if (options != null && options.containsKey("accountId")) {
            jpqlBuilder.append(" AND t.account.id = :accountId");
        }

        // Add location filter if provided
        if (options != null && options.containsKey("location")) {
            jpqlBuilder.append(" AND LOWER(t.location) LIKE :locationPattern");
        }

        if (options != null) {
            if (options.containsKey("minAmount")) {
                jpqlBuilder.append(" AND t.amount >= :minAmount");
            }
            if (options.containsKey("maxAmount")) {
                jpqlBuilder.append(" AND t.amount <= :maxAmount");
            }
        }

        // Add sorting if provided
        if (options != null && options.containsKey("sortBy")) {
            String sortBy = (String) options.get("sortBy");
            String sortDirection = (String) options.getOrDefault("sortDirection", "ASC");

            if (isValidTransactionSortField(sortBy)) {
                jpqlBuilder.append(" ORDER BY t.").append(sortBy).append(" ").append(sortDirection);
            }
        } else {
            jpqlBuilder.append(" ORDER BY t.transactionDate DESC"); // default sort by date
        }

        TypedQuery<Transaction> query = entityManager.createQuery(jpqlBuilder.toString(), Transaction.class);

        if (!"ADMIN".equals(role)) {
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

        if (options != null && options.containsKey("location")) {
            String location = (String) options.get("location");
            query.setParameter("locationPattern", "%" + location.toLowerCase() + "%");
        }

        if (options != null && options.containsKey("accountId")) {
            query.setParameter("accountId", options.get("accountId"));
        }

        if (options != null) {
            if (options.containsKey("minAmount")) {
                query.setParameter("minAmount", BigDecimal.valueOf((Double) options.get("minAmount")));
            }
            if (options.containsKey("maxAmount")) {
                query.setParameter("maxAmount", BigDecimal.valueOf((Double) options.get("maxAmount")));
            }
        }


        List<Transaction> transactions = query
                .setFirstResult(pageNumber * limit)
                .setMaxResults(limit)
                .getResultList();

        // Build count query
        StringBuilder countJpqlBuilder = new StringBuilder("SELECT COUNT(t) FROM Transaction t");

        if (!"ADMIN".equals(role)) {
            countJpqlBuilder.append(" WHERE t.account IN (SELECT a FROM Account a WHERE a.customer.id = :customerId) AND ");
        } else {
            countJpqlBuilder.append(" WHERE ");
        }

        countJpqlBuilder.append("(:keyword IS NULL OR ")
                .append("t.id LIKE :searchPattern OR ")
                .append("LOWER(t.description) LIKE :searchPattern OR ")
                .append("LOWER(t.type) LIKE :searchPattern OR ")
                .append("LOWER(t.location) LIKE :searchPattern)");

        if (options != null && options.containsKey("accountId")) {
            countJpqlBuilder.append(" AND t.account.id = :accountId");
        }

        // Add location filter for count query too
        if (options != null && options.containsKey("location")) {
            countJpqlBuilder.append(" AND LOWER(t.location) LIKE :locationPattern");
        }

        if (options != null) {
            if (options.containsKey("minAmount")) {
                countJpqlBuilder.append(" AND t.amount >= :minAmount");
            }
            if (options.containsKey("maxAmount")) {
                countJpqlBuilder.append(" AND t.amount <= :maxAmount");
            }
        }


        TypedQuery<Long> countQuery = entityManager.createQuery(countJpqlBuilder.toString(), Long.class);

        if (!"ADMIN".equals(role)) {
            countQuery.setParameter("customerId", customerId);
        }

        if (StringUtils.hasText(keyword)) {
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            countQuery.setParameter("keyword", keyword);
            countQuery.setParameter("searchPattern", searchPattern);
        } else {
            countQuery.setParameter("keyword", null);
            countQuery.setParameter("searchPattern", null);
        }

        if (options != null && options.containsKey("location")) {
            String location = (String) options.get("location");
            countQuery.setParameter("locationPattern", "%" + location.toLowerCase() + "%");
        }

        if (options != null) {
            if (options.containsKey("minAmount")) {
                countQuery.setParameter("minAmount", BigDecimal.valueOf((Double) options.get("minAmount")));
            }
            if (options.containsKey("maxAmount")) {
                countQuery.setParameter("maxAmount", BigDecimal.valueOf((Double) options.get("maxAmount")));
            }
        }

        if (options != null && options.containsKey("accountId")) {
            countQuery.setParameter("accountId", options.get("accountId"));
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

        return paginDto;
    }

    private boolean isValidTransactionSortField(String sortBy) {
        return sortBy != null && (
                sortBy.equals("amount") ||
                        sortBy.equals("type") ||
                        sortBy.equals("transactionDate") ||
                        sortBy.equals("location") ||
                        sortBy.equals("description")
        );
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
    public TransactionResponseDto transferMoney(MoneyTransferRequestDto request, String customerId) {
        String fromAccountId = request.getFromAccountId();
        String toAccountId = request.getToAccountId();
        String description = request.getDescription();

        if (fromAccountId.equals(toAccountId)) {
            throw new RuntimeException("Account can't be the same");
        }

        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new RuntimeException("From account does not exists"));
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new RuntimeException("To account does not exists"));

        if (!fromAccount.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("This account does not belong to this customers");
        }

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Source account must be active");
        }

        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
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

        toAccount.setBalanceType(BalanceType.valueOf(BalanceTypeUtils.validateBalanceStatus(toAccount)));

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
        fromTransaction.setFromAccountId(fromAccount.getId());
        fromTransaction.setToAccountId(toAccount.getId());

        Transaction toTransaction = new Transaction();
        toTransaction.setAccount(toAccount);
        toTransaction.setAmount(amount);
        toTransaction.setDescription(description);
        toTransaction.setLocation(request.getLocation());
        toTransaction.setType(TransactionType.TRANSFER_IN);
        toTransaction.setTransactionDate(transactionDate);
        toTransaction.setFromAccountId(fromAccount.getId());
        toTransaction.setToAccountId(toAccount.getId());

        Transaction savedTransaction = transactionRepository.save(fromTransaction);
        transactionRepository.save(toTransaction);

        // Save accounts
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Kiểm tra giao dịch bất thường
        alertService.isTransactionAbnormal(fromTransaction);
        alertService.isTransactionAbnormal(toTransaction);

        return TransactionResponseDto.build(savedTransaction);
    }

    @Override
    @Transactional
    public TransactionResponseDto depositMoney(MoneyUpdateRequest request, String customerId) {
        Account account = accountRepository.findById(request.getAccountId()).orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("This account does not belong to this customers");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account must be active");
        }

        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        account.setBalance(account.getBalance().add(amount));

        account.setBalanceType(BalanceType.valueOf(BalanceTypeUtils.validateBalanceStatus(account)));

        Date transactionDate = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionDate(transactionDate);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setLocation(request.getLocation());
        transaction.setDescription("Deposit money into wallet");
        transaction.setToAccountId(account.getId());
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Kiểm tra giao dịch bất thường
        alertService.isTransactionAbnormal(transaction);
        return TransactionResponseDto.build(savedTransaction);
    }

    @Override
    @Transactional
    public TransactionResponseDto withdrawMoney(MoneyUpdateRequest request, String customerId) {
        Account account = accountRepository.findById(request.getAccountId()).orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("This account does not belong to this customers");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account must be active");
        }

        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Your account balance is not enough");
        }

        account.setBalance(account.getBalance().subtract(amount));

        account.setBalanceType(BalanceType.valueOf(BalanceTypeUtils.validateBalanceStatus(account)));

        Date transactionDate = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionDate(transactionDate);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.WITHDRAW);
        transaction.setLocation(request.getLocation());
        transaction.setDescription("Withdraw money from wallet");
        transaction.setFromAccountId(account.getId());
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Kiểm tra giao dịch bất thường
        alertService.isTransactionAbnormal(transaction);

        return TransactionResponseDto.build(savedTransaction);
    }
}
