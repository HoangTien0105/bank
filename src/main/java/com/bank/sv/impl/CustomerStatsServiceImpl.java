package com.bank.sv.impl;

import com.bank.constant.Message;
import com.bank.dto.response.CustomerStatsResponseDto;
import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.model.CustomerStatistics;
import com.bank.model.Transaction;
import com.bank.repository.AccountRepository;
import com.bank.repository.CustomerRepository;
import com.bank.repository.CustomerStatsRepository;
import com.bank.repository.TransactionRepository;
import com.bank.sv.CustomerStatsService;
import com.bank.utils.ExcelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomerStatsServiceImpl implements CustomerStatsService {

    @Autowired
    private CustomerStatsRepository customerStatsRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    @Transactional
    public void generateStatsForMonth(Integer year, Integer month) {
        //Init YearMonth
        YearMonth yearMonth = YearMonth.of(year, month);

        LocalDateTime startOfMoth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        //Chuyển thành date để repo query
        Date startDate = Date.from(startOfMoth.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
        Date endDate = Date.from(endOfMonth.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());

        List<Customer> customers = customerRepository.findAll();

        for (Customer customer : customers) {
            Optional<CustomerStatistics> existedStats = customerStatsRepository.findByYearAndMonthAndCustomerId(year, month, customer.getId());

            if (existedStats.isPresent()) continue;

            Account account = accountRepository.findCheckingAccountByCustomerId(customer.getId());

            if (account == null) continue;

            BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;

            List<Transaction> transactions = transactionRepository.findByAccountAndTransactionDateBetween(account, startDate, endDate);

            //Nếu k có giao dịch => vẫn lưu nhưng data = 0
            if (transactions.isEmpty()) {
                CustomerStatistics stats = CustomerStatistics.builder()
                        .year(year)
                        .month(month)
                        .customer(customer)
                        .totalTransactions(0L)
                        .maxTransactionAmount(BigDecimal.ZERO)
                        .avgTransactionAmount(BigDecimal.ZERO)
                        .minTransactionAmount(BigDecimal.ZERO)
                        .endMonthBalance(balance)
                        .totalTransactionAmount(BigDecimal.ZERO)
                        .build();

                customerStatsRepository.save(stats);
                continue;
            }

            Long totalTransactions = (long) transactions.size();

            BigDecimal maxAmount = transactions.stream()
                    .map(Transaction::getAmount)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal minAmount = transactions.stream()
                    .map(Transaction::getAmount)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal totalAmount = transactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avgAmount = totalAmount.divide(new BigDecimal(totalTransactions), 2, RoundingMode.HALF_UP);

            CustomerStatistics stats = CustomerStatistics.builder()
                    .year(year)
                    .month(month)
                    .customer(customer)
                    .totalTransactions(totalTransactions)
                    .maxTransactionAmount(maxAmount)
                    .avgTransactionAmount(avgAmount)
                    .minTransactionAmount(minAmount)
                    .endMonthBalance(balance)
                    .totalTransactionAmount(totalAmount)
                    .build();

            customerStatsRepository.save(stats);
        }
    }

    @Override
    @Cacheable(value = "customers_stats", key = "#customers_stats")
    public List<CustomerStatsResponseDto> getStatsForCustomer(String cusId, Date startDate, Date endDate) {
        LocalDate startLocalDate = startDate.toInstant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
        LocalDate endLocalDate = endDate.toInstant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();

        int startYear = startLocalDate.getYear();
        int startMonth = startLocalDate.getMonthValue();
        int endYear = endLocalDate.getYear();
        int endMonth = endLocalDate.getMonthValue();

        List<CustomerStatistics> customerStatistics = customerStatsRepository.findByCustomerIdAndDateRange(
                cusId, startYear, startMonth, endYear, endMonth);

        return customerStatistics.stream()
                .map(stats -> CustomerStatsResponseDto.builder()
                        .id(stats.getId())
                        .year(stats.getYear())
                        .month(stats.getMonth())
                        .customer(stats.getCustomer().getId())
                        .totalTransactions(stats.getTotalTransactions())
                        .maxTransactionAmount(stats.getMaxTransactionAmount())
                        .avgTransactionAmount(stats.getAvgTransactionAmount())
                        .minTransactionAmount(stats.getMinTransactionAmount())
                        .endMonthBalance(stats.getEndMonthBalance())
                        .totalTransactionAmount(stats.getTotalTransactionAmount())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerStatsResponseDto> getStatsForMonth(Integer year, Integer month) {
        List<CustomerStatistics> customerStatistics = customerStatsRepository.findAllByYearAndMonth(year, month);

        return customerStatistics.stream()
                .map(stats -> CustomerStatsResponseDto.builder()
                        .id(stats.getId())
                        .year(stats.getYear())
                        .month(stats.getMonth())
                        .customer(stats.getCustomer().getId())
                        .totalTransactions(stats.getTotalTransactions())
                        .maxTransactionAmount(stats.getMaxTransactionAmount())
                        .avgTransactionAmount(stats.getAvgTransactionAmount())
                        .minTransactionAmount(stats.getMinTransactionAmount())
                        .endMonthBalance(stats.getEndMonthBalance())
                        .totalTransactionAmount(stats.getTotalTransactionAmount())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public byte[] exportToExcel(Integer year, Integer month) {
        List<CustomerStatsResponseDto> statsList;

        if (year != null && month != null) {
            statsList = getStatsForMonth(year, month);
            if (statsList.isEmpty()) {
                throw new RuntimeException("No data found for " + month + "/" + year);
            }
        } else if (year != null) {
            List<CustomerStatistics> yearStats = customerStatsRepository.findAllByYear(year);
            if (yearStats.isEmpty()) {
                throw new RuntimeException("No data found for " + year);
            }

            statsList = yearStats.stream()
                    .map(stats -> CustomerStatsResponseDto.builder()
                            .id(stats.getId())
                            .year(stats.getYear())
                            .month(stats.getMonth())
                            .customer(stats.getCustomer().getId())
                            .totalTransactions(stats.getTotalTransactions())
                            .maxTransactionAmount(stats.getMaxTransactionAmount())
                            .avgTransactionAmount(stats.getAvgTransactionAmount())
                            .minTransactionAmount(stats.getMinTransactionAmount())
                            .endMonthBalance(stats.getEndMonthBalance())
                            .totalTransactionAmount(stats.getTotalTransactionAmount())
                            .build())
                    .collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException(Message.YEAR_REQUIRED);
        }

        return ExcelUtils.createCustomerStatsExcel(statsList, year, month);
    }

    @Override
    public byte[] exportWeeklyStatsToExcel(String customerId, int year) {
        if (customerId == null || customerId.isEmpty()) {
            throw new IllegalArgumentException(Message.CUS_ID_REQUIRED);
        }

        // Lấy tất cả thống kê của khách hàng trong năm
        List<CustomerStatistics> allYearStats = customerStatsRepository.findAllByYear(year);

        // Lọc theo customerId
        List<CustomerStatistics> customerYearStats = allYearStats.stream()
                .filter(stats -> stats.getCustomer().getId().equals(customerId))
                .collect(Collectors.toList());

        if (customerYearStats.isEmpty()) {
            throw new RuntimeException("Data not found for customer " + customerId + " in year " + year);
        }

        // Nhóm thống kê theo tuần
        Map<Integer, List<CustomerStatistics>> weeklyStats = customerYearStats.stream()
                .collect(Collectors.groupingBy(stats -> {
                    // Chuyển đổi tháng và ngày thành tuần trong năm
                    LocalDate date = LocalDate.of(stats.getYear(), stats.getMonth(), 15); // Lấy ngày giữa tháng
                    return date.get(WeekFields.of(Locale.getDefault()).weekOfYear());
                }));

        List<CustomerStatsResponseDto> weeklyAggregatedStats = new ArrayList<>();

        for (Map.Entry<Integer, List<CustomerStatistics>> entry : weeklyStats.entrySet()) {
            Integer weekNumber = entry.getKey();
            List<CustomerStatistics> weekStats = entry.getValue();

            if (weekStats.isEmpty()) continue;

            // Lấy thông tin khách hàng từ thống kê đầu tiên
            CustomerStatistics firstStat = weekStats.get(0);

            Long totalTransactions = weekStats.stream().mapToLong(CustomerStatistics::getTotalTransactions).sum();

            BigDecimal maxAmount = weekStats.stream()
                    .map(CustomerStatistics::getMaxTransactionAmount)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal minAmount = weekStats.stream()
                    .map(CustomerStatistics::getMinTransactionAmount)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal totalWeightedAvg = BigDecimal.ZERO;
            Long totalWeightedCount = 0L;

            for (CustomerStatistics stats : weekStats) {
                if (stats.getAvgTransactionAmount() != null && stats.getTotalTransactions() > 0) {
                    totalWeightedAvg = totalWeightedAvg.add(
                            stats.getAvgTransactionAmount().multiply(new BigDecimal(stats.getTotalTransactions())));
                    totalWeightedCount += stats.getTotalTransactions();
                }
            }

            BigDecimal avgAmount = totalWeightedCount > 0 ?
                    totalWeightedAvg.divide(new BigDecimal(totalWeightedCount), 2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            BigDecimal endBalance = weekStats.stream()
                    .max(Comparator.comparing(CustomerStatistics::getMonth))
                    .map(CustomerStatistics::getEndMonthBalance)
                    .orElse(BigDecimal.ZERO);

            BigDecimal totalAmount = weekStats.stream()
                    .map(CustomerStatistics::getTotalTransactionAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            CustomerStatsResponseDto weeklyStatsDto = CustomerStatsResponseDto.builder()
                    .id(firstStat.getId())
                    .year(year)
                    .month(weekNumber) // Sử dụng số tuần thay cho tháng
                    .customer(customerId)
                    .totalTransactions(totalTransactions)
                    .maxTransactionAmount(maxAmount)
                    .avgTransactionAmount(avgAmount)
                    .minTransactionAmount(minAmount)
                    .endMonthBalance(endBalance)
                    .totalTransactionAmount(totalAmount)
                    .build();

            weeklyAggregatedStats.add(weeklyStatsDto);
        }

        weeklyAggregatedStats.sort(Comparator.comparing(CustomerStatsResponseDto::getMonth));

        return ExcelUtils.createCustomerStatsExcel(weeklyAggregatedStats, year, null);
    }

    @Override
    public byte[] exportQuarterlyStatsToExcel(String customerId, int year) {

        if (customerId == null || customerId.isEmpty()) {
            throw new IllegalArgumentException(Message.CUS_ID_REQUIRED);
        }

        List<CustomerStatistics> customerYearStats = customerStatsRepository.findByYearAndCustomerId(year, customerId);

        if (customerYearStats.isEmpty()) {
            throw new RuntimeException("Data not found for customer " + customerId + " in year " + year);
        }

        // Nhóm thống kê theo quý
        Map<Integer, List<CustomerStatistics>> quarterlyStats = customerYearStats.stream()
                .collect(Collectors.groupingBy(stats -> {
                    int month = stats.getMonth();
                    return (month - 1) / 3 + 1; // Chuyển đổi tháng thành quý (1-4)
                }));

        List<CustomerStatsResponseDto> quarterlyAggregatedStats = new ArrayList<>();

        for (int quarter = 1; quarter <= 4; quarter++) {
            List<CustomerStatistics> quarterStats = quarterlyStats.getOrDefault(quarter, Collections.emptyList());

            if (quarterStats.isEmpty()) continue;

            CustomerStatistics firstStat = quarterStats.get(0);

            Long totalTransactions = quarterStats.stream().mapToLong(CustomerStatistics::getTotalTransactions).sum();

            BigDecimal maxAmount = quarterStats.stream()
                    .map(CustomerStatistics::getMaxTransactionAmount)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal minAmount = quarterStats.stream()
                    .map(CustomerStatistics::getMinTransactionAmount)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal totalWeightedAvg = BigDecimal.ZERO;
            Long totalWeightedCount = 0L;

            for (CustomerStatistics stats : quarterStats) {
                if (stats.getAvgTransactionAmount() != null && stats.getTotalTransactions() > 0) {
                    totalWeightedAvg = totalWeightedAvg.add(
                            stats.getAvgTransactionAmount().multiply(new BigDecimal(stats.getTotalTransactions())));
                    totalWeightedCount += stats.getTotalTransactions();
                }
            }

            BigDecimal avgAmount = totalWeightedCount > 0 ?
                    totalWeightedAvg.divide(new BigDecimal(totalWeightedCount), 2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            BigDecimal endBalance = quarterStats.stream()
                    .max(Comparator.comparing(CustomerStatistics::getMonth))
                    .map(CustomerStatistics::getEndMonthBalance)
                    .orElse(BigDecimal.ZERO);

            BigDecimal totalAmount = quarterStats.stream()
                    .map(CustomerStatistics::getTotalTransactionAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            CustomerStatsResponseDto quarterlyStatsDto = CustomerStatsResponseDto.builder()
                    .id(firstStat.getId())
                    .year(year)
                    .month(quarter) // Sử dụng số quý thay cho tháng
                    .customer(customerId)
                    .totalTransactions(totalTransactions)
                    .maxTransactionAmount(maxAmount)
                    .avgTransactionAmount(avgAmount)
                    .minTransactionAmount(minAmount)
                    .endMonthBalance(endBalance)
                    .totalTransactionAmount(totalAmount)
                    .build();

            quarterlyAggregatedStats.add(quarterlyStatsDto);
        }

        quarterlyAggregatedStats.sort(Comparator.comparing(CustomerStatsResponseDto::getMonth));

        return ExcelUtils.createCustomerStatsExcel(quarterlyAggregatedStats, year, null);
    }

    @Override
    public byte[] exportYearlyStatsToExcel(String customerId, int year) {
        if (customerId == null || customerId.isEmpty()) {
            throw new IllegalArgumentException(Message.CUS_ID_REQUIRED);
        }

        List<CustomerStatistics> yearStats = customerStatsRepository.findByYearAndCustomerId(year, customerId);

        if (yearStats.isEmpty()) {
            throw new RuntimeException("Data not found for " + customerId + " in " + year);
        }

        Long totalTransactions = yearStats.stream().mapToLong(CustomerStatistics::getTotalTransactions).sum();

        BigDecimal maxAmount = yearStats.stream()
                .map(CustomerStatistics::getMaxTransactionAmount)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal minAmount = yearStats.stream()
                .map(CustomerStatistics::getMinTransactionAmount)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalAvg = BigDecimal.ZERO;
        Long totalAvgCount = 0L;

        for(CustomerStatistics stats: yearStats) {
            if (stats.getAvgTransactionAmount() != null && stats.getTotalTransactions() > 0) {
                totalAvg = totalAvg.add(
                        stats.getAvgTransactionAmount().multiply(new BigDecimal(stats.getTotalTransactions())));
                totalAvgCount += stats.getTotalTransactions();
            }
        }

        BigDecimal avgAmount = totalAvgCount > 0 ?
                totalAvg.divide(new BigDecimal(totalAvgCount), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        BigDecimal endYearBalance = yearStats.stream()
                .max(Comparator.comparing(CustomerStatistics::getMonth))
                .map(CustomerStatistics::getEndMonthBalance)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalAmountTransactions = yearStats.stream()
                .map(CustomerStatistics::getTotalTransactionAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Customer customer = yearStats.get(0).getCustomer();
        CustomerStatsResponseDto yearlyStats = CustomerStatsResponseDto.builder()
                .year(year)
                .month(0) // 0 đại diện cho cả năm
                .customer(customer.getId())
                .totalTransactions(totalTransactions)
                .maxTransactionAmount(maxAmount)
                .avgTransactionAmount(avgAmount)
                .minTransactionAmount(minAmount)
                .endMonthBalance(endYearBalance)
                .totalTransactionAmount(totalAmountTransactions)
                .build();

        List<CustomerStatsResponseDto> statsList = new ArrayList<>();
        statsList.add(yearlyStats);

        String period = "Yearly Report " + year;
        return ExcelUtils.createCustomerStatsExcel(statsList, year, null);
    }
}
