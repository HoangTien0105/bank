package com.bank.sv.impl;

import com.bank.constant.Message;
import com.bank.dto.PaginDto;
import com.bank.dto.request.MoneyTransferRequestDto;
import com.bank.dto.response.TransactionResponseDto;
import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PaginDto<TransactionResponseDto> getTransactions(PaginDto<TransactionResponseDto> pagin) {

        int offset = pagin.getOffset() != null ? pagin.getOffset() : 0;
        int limit = pagin.getLimit() != null ? pagin.getLimit() : 10;

        String keyword = pagin.getKeyword();

        int pageNumber = offset / limit;

        String jpql = "SELECT t FROM Transaction t WHERE " +
                "(:keyword IS NULL OR " +
                "LOWER(a.description) LIKE :searchPattern OR " +
                "LOWER(a.type) LIKE :searchPattern OR " +
                "LOWER(a.location) LIKE :searchPattern)";

        TypedQuery<Transaction> query = entityManager.createQuery(jpql, Transaction.class);

        if (StringUtils.hasText(keyword)) {
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            query.setParameter("keyword", keyword);
            query.setParameter("searchPattern", searchPattern);
        } else {
            query.setParameter("keyword", null);
            query.setParameter("searchPattern", null);
        }

        List<Transaction> transactions = query
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        String countJpql = "SELECT COUNT(t) FROM Transaction t WHERE " +
                "(:keyword IS NULL OR " +
                "LOWER(a.description) LIKE :searchPattern OR " +
                "LOWER(a.type) LIKE :searchPattern OR " +
                "LOWER(a.location) LIKE :searchPattern)";

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

        List<TransactionResponseDto> response = transactions.stream()
                .map(TransactionResponseDto::build)
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
    public TransactionResponseDto getTransactionById(String id) {
        Transaction transaction = transactionRepository.findById(id).orElseThrow(() -> new RuntimeException(Message.TRANSACTION_NOT_FOUND));
        return TransactionResponseDto.build(transaction);
    }

    @Override
    public void transferMoney(MoneyTransferRequestDto request) {
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
            throw new RuntimeException("Số tiền vượt quá giới hạn giao dịch của tài khoản");
        }

        //Update balance
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        // Update status and transaction limit
        fromAccount.setStatus(AccountStatus.valueOf(BalanceTypeUtils.validateAccountStatus(fromAccount)));
        BalanceTypeUtils.setTransactionLimitBasedOnBalance(fromAccount);

        toAccount.setStatus(AccountStatus.valueOf(BalanceTypeUtils.validateAccountStatus(toAccount)));
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
}
