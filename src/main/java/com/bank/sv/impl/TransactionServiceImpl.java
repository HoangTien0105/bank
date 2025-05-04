package com.bank.sv.impl;

import com.bank.constant.Message;
import com.bank.dto.PaginDto;
import com.bank.dto.response.TransactionResponseDto;
import com.bank.model.Transaction;
import com.bank.repository.TransactionRepository;
import com.bank.sv.TransactionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

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
}
