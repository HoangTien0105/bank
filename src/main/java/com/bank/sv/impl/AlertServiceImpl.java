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
import com.bank.repository.TransactionRepository;
import com.bank.sv.AlertService;
import com.bank.utils.CustomerTypeUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AlertServiceImpl implements AlertService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EntityManager entityManager;

    @Value("${transaction.alert.time.threshold:300}")
    private int timeThresholdSeconds; // Mặc định 5 phút (300 giây)

    private BigDecimal getAmountThreshold(CustomerType customerType) {
        if (customerType == null) {
            return com.bank.constant.Value.PERSONAL;
        }
        return CustomerTypeUtils.getAlertThresholdByCustomerType(customerType);
    }

    @Override
    @Transactional
    public void detectAbnormalTransactions() {
        // Lấy các giao dịch gần đây chưa được kiểm tra
        Date checkTime = new Date();
        LocalDateTime localCheckTime = checkTime.toInstant()
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime();
        LocalDateTime startTime = localCheckTime.minusDays(2); // Kiểm tra trogn 1 ngy gần nhất

        Date startDate = Date.from(startTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());

        List<Transaction> transactions = transactionRepository.findAllByTransactionDateBetweenWithCustomerType(startDate, checkTime);

        // Sử dụng Virtual Thread để xử lý danh sách giao dịch
        List<CompletableFuture<Boolean>> futures = transactions.stream()
                .filter(transaction -> !alertRepository.existsByTransactionId(transaction.getId()))
                .map(transaction -> CompletableFuture.supplyAsync(() -> { //Tạo 1 task bất đồng bộ để trả về kết quả
                    try {
                        return processTransaction(transaction);
                    } catch (Exception e) {
                        System.err.println("Error processing transaction: " + e.getMessage());
                        return false;
                    }
                })).collect(Collectors.toList());
        if (!futures.isEmpty()) {
            try {
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0]));

                allFutures.join();
            } catch (Exception e) {
                System.err.println("Error waiting for transaction processing: " + e.getMessage());
            }
        }
    }

    private boolean processTransaction(Transaction transaction) {
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

        if (rencentAccountWithTransactions.size() > 3) {
            createAlert(transaction, AlertType.RAPID_TRANSACTIONS,
                    "Detected " + rencentAccountWithTransactions.size() + " transactions within " + timeThresholdSeconds
                            + " seconds from account " + transaction.getAccount().getId() + " in " + transaction.getTransactionDate());
            isAbnormal = true;
        }

        return isAbnormal;
    }

    @Override
    public PaginDto<AlertResponseDto> getAlerts(PaginDto<AlertResponseDto> paginDto, String role) {
        int offset = paginDto.getOffset() != null ? paginDto.getOffset() : 0;
        int limit = paginDto.getLimit() != null ? paginDto.getLimit() : 10;
        String keyword = paginDto.getKeyword();
        int pageNumber = offset / limit;
        Map<String, Object> options = paginDto.getOptions();

        StringBuilder jpql = new StringBuilder("SELECT a FROM Alert a WHERE 1=1");
        StringBuilder countJpql = new StringBuilder("SELECT COUNT(a) FROM Alert a WHERE 1=1");

        if (StringUtils.hasText(keyword)) {
            jpql.append(" AND LOWER(a.description) LIKE :searchPattern");
            countJpql.append(" AND LOWER(a.description) LIKE :searchPattern");
        }

        if (options != null && options.containsKey("type")) {
            jpql.append(" AND a.alertType = :alertType");
            countJpql.append(" AND a.alertType = :alertType");
        }

        if (options != null && options.containsKey("status")) {
            jpql.append(" AND a.status = :status");
            countJpql.append(" AND a.status = :status");
        }

        jpql.append(" ORDER BY a.processedDate DESC NULLS LAST, a.createDate DESC");

        TypedQuery<Alert> query = entityManager.createQuery(jpql.toString(), Alert.class);

        if (StringUtils.hasText(keyword)) {
            query.setParameter("searchPattern", "%" + keyword.toLowerCase() + "%");
        }

        if (options != null && options.containsKey("type")) {
            String type = (String) options.get("type");
            try {
                AlertType alertType = AlertType.valueOf(type.toUpperCase());
                query.setParameter("alertType", alertType);
            } catch (RuntimeException e) {
                query.setParameter("alertType", null);
            }
        }

        if (options != null && options.containsKey("status")) {
            String status = (String) options.get("status");
            try {
                AlertStatus alertStatus = AlertStatus.valueOf(status.toUpperCase());
                query.setParameter("status", alertStatus);
            } catch (RuntimeException e) {
                query.setParameter("status", null);
            }
        }

        // Pagination
        query.setFirstResult(pageNumber * limit);
        query.setMaxResults(limit);

        List<Alert> alerts = query.getResultList();

        // Tạo count query
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql.toString(), Long.class);

        if (StringUtils.hasText(keyword)) {
            countQuery.setParameter("searchPattern", "%" + keyword.toLowerCase() + "%");
        }

        if (options != null && options.containsKey("type")) {
            String type = (String) options.get("type");
            try {
                AlertType alertType = AlertType.valueOf(type.toUpperCase());
                countQuery.setParameter("alertType", alertType);
            } catch (RuntimeException e) {
                countQuery.setParameter("alertType", null);
            }
        }

        if (options != null && options.containsKey("status")) {
            String status = (String) options.get("status");
            try {
                AlertStatus alertStatus = AlertStatus.valueOf(status.toUpperCase());
                countQuery.setParameter("status", alertStatus);
            } catch (IllegalArgumentException e) {
                countQuery.setParameter("status", null);
            }
        }

        Long totalRows = countQuery.getSingleResult();

        List<AlertResponseDto> result = alerts.stream()
                .map(AlertResponseDto::build)
                .collect(Collectors.toList());

        // Set pagination info
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
    @Async
    public void isTransactionAbnormal(Transaction transaction) {
        CustomerType customerTypeName = transaction.getAccount().getCustomer().getType();
        BigDecimal threshold = getAmountThreshold(customerTypeName);

        //Kiểm tra số tiền
        if (transaction.getAmount().compareTo(threshold) > 0) {
            createAlert(transaction, AlertType.LARGE_AMOUNT,
                    "Transaction with an amount exceeding the threshold (" + transaction.getAmount()
                            + ") from account " + transaction.getAccount().getId() + " in " + transaction.getTransactionDate());
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
        }
    }

    @Async
    public void checkAndCreateAlert(Transaction transaction) {
        try {
            isTransactionAbnormal(transaction);
        } catch (Exception e) {
            System.err.println("Error checking for abnormal transaction: " + e.getMessage());
        }
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
