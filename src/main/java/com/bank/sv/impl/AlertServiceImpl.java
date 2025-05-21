package com.bank.sv.impl;

import com.bank.constant.Message;
import com.bank.dto.PaginDto;
import com.bank.dto.response.AlertResponseDto;
import com.bank.enums.AlertStatus;
import com.bank.enums.AlertType;
import com.bank.model.Alert;
import com.bank.model.CustomerType;
import com.bank.model.Transaction;
import com.bank.repository.AlertRepository;
import com.bank.repository.CustomerTypeRepository;
import com.bank.repository.TransactionRepository;
import com.bank.sv.AlertService;
import com.bank.utils.CustomerTypeUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertServiceImpl implements AlertService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CustomerTypeRepository customerTypeRepository;

    @Autowired
    private EntityManager entityManager;

    @Value("${transaction.alert.time.threshold:300}")
    private int timeThresholdSeconds; // Mặc định 5 phút (300 giây)

    private BigDecimal getAmountThreshold(CustomerType customerType) {
        return CustomerTypeUtils.getAlertThresholdByCustomerType(customerType);
    }

    @Override
    @Transactional
    public void detectAbnormalTransactions() {
        // Lấy các giao dịch gần đây chưa được kiểm tra
        Date checkTime = new Date();
        LocalDateTime localCheckTime = checkTime.toInstant()
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime();
        LocalDateTime startTime = localCheckTime.minusDays(1); // Kiểm tra trogn 1 ngy gần nhất

        Date startDate = Date.from(startTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());

        List<Transaction> transactions = transactionRepository.findAllByTransactionDateBetween(startDate, checkTime);

        for (Transaction transaction : transactions) {
            // Kiểm tra xem giao dịch đã được cảnh báo chưa
            if (!alertRepository.existsByTransactionId(transaction.getId())) {
                if (isTransactionAbnormal(transaction)) {
                    // Đã được xử lý trong phương thức isTransactionAbnormal
                }
            }
        }
    }

    @Override
    public PaginDto<AlertResponseDto> getAlerts(PaginDto<AlertResponseDto> paginDto, String role) {
        int offset = paginDto.getOffset() != null ? paginDto.getOffset() : 0;
        int limit = paginDto.getLimit() != null ? paginDto.getLimit() : 10;
        String keyword = paginDto.getKeyword();
        int pageNumber = offset / limit;
        AlertType alertType = null;
        AlertStatus alertStatus = null;

        // Try to convert keyword to enums if present
        if (StringUtils.hasText(keyword)) {
            try {
                alertType = AlertType.valueOf(keyword.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore conversion error
            }

            try {
                alertStatus = AlertStatus.valueOf(keyword.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore conversion error
            }
        }

        String jpql = "SELECT a FROM Alert a WHERE " +
                "(:keyword IS NULL OR " +
                "LOWER(a.description) LIKE :searchPattern OR " +
                "(:alertType IS NULL OR a.alertType = :alertType) OR " +
                "(:status IS NULL OR a.status = :status))";

        TypedQuery<Alert> query = entityManager.createQuery(jpql, Alert.class);

        query.setParameter("keyword", StringUtils.hasText(keyword) ? keyword : null);
        query.setParameter("searchPattern", StringUtils.hasText(keyword) ? "%" + keyword.toLowerCase() + "%" : null);
        query.setParameter("alertType", alertType);
        query.setParameter("status", alertStatus);

        query.setFirstResult(pageNumber * limit);
        query.setMaxResults(limit);

        List<Alert> alerts = query.getResultList();

        String countJpql = jpql.replace("SELECT a", "SELECT COUNT(a)");
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        countQuery.setParameter("keyword", StringUtils.hasText(keyword) ? keyword : null);
        countQuery.setParameter("searchPattern", StringUtils.hasText(keyword) ? "%" + keyword.toLowerCase() + "%" : null);
        countQuery.setParameter("alertType", alertType);
        countQuery.setParameter("status", alertStatus);

        Long totalRows = countQuery.getSingleResult();

        List<AlertResponseDto> result = alerts.stream()
                .map(AlertResponseDto::build)
                .collect(Collectors.toList());

        paginDto.setResults(result);
        paginDto.setOffset(offset);
        paginDto.setLimit(limit);
        paginDto.setPageNumber(pageNumber + 1);
        paginDto.setTotalRows(totalRows);
        paginDto.setTotalPages((int) Math.ceil((double) totalRows / limit));

        return paginDto;
    }

    @Override
    public AlertResponseDto getAlertById(String id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(Message.ALERT_NOT_FOUND));
        return AlertResponseDto.build(alert);
    }

    @Override
    @Transactional
    public void updateAlertStatus(String id, String status, String processedBy, String notes) {
        try {
            AlertStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(Message.ALERT_STATUS_INVALID);
        }

        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(Message.ALERT_NOT_FOUND));

        alert.setStatus(AlertStatus.valueOf(status));
        alert.setProcessedBy(processedBy);
        alert.setResolutionNotes(notes);
        alert.setProcessedDate(new Date());

        alertRepository.save(alert);
    }

    @Override
    public boolean isTransactionAbnormal(Transaction transaction) {
        boolean isAbnormal = false;

        CustomerType customerTypeName = transaction.getAccount().getCustomer().getType();
        BigDecimal threshold = getAmountThreshold(customerTypeName);

        //Kiểm tra số tiền
        if (transaction.getAmount().compareTo(threshold) > 0) {
            createAlert(transaction, AlertType.LARGE_AMOUNT,
                    "Transaction with an amount exceeding the threshold (" + transaction.getAmount()
                            + ") from account " + transaction.getAccount().getId() + " in " + transaction.getTransactionDate());
            isAbnormal = true;
        }

        //Kiểm tra giao dịch có xảy ra liên tục trong 1 khoảng thời gian ngắn không
        LocalDateTime transactionTime = transaction.getTransactionDate().toInstant()
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .toLocalDateTime();
        LocalDateTime rapidCheckTime = transactionTime.minusSeconds(timeThresholdSeconds);
        Date rapidStartTime = Date.from(rapidCheckTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());

        List<Transaction> rencentAccountWithTransactions = transactionRepository
                .findByAccountAndTransactionDateBetween(transaction.getAccount(), rapidStartTime, transaction.getTransactionDate());

        // Nếu có nhiều hơn 10 giao dịch trong khoảng thời gian 10 phút
        // Business: 10, Demo: 3
        if (rencentAccountWithTransactions.size() > 3) {
            createAlert(transaction, AlertType.RAPID_TRANSACTIONS,
                    "Detected " + rencentAccountWithTransactions.size() + " transactions within " + timeThresholdSeconds
                            + " seconds from account " + transaction.getAccount().getId() + " in " + transaction.getTransactionDate());
            isAbnormal = true;
        }

        return isAbnormal;
    }

    private void createAlert(Transaction transaction, AlertType alertType, String description) {
        Alert alert = Alert.builder()
                .transaction(transaction)
                .alertType(alertType)
                .description(description)
                .status(AlertStatus.NEW)
                .build();

        alertRepository.save(alert);
    }
}
