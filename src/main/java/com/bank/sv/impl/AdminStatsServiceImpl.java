package com.bank.sv.impl;

import com.bank.constant.Message;
import com.bank.model.AdminStatistics;
import com.bank.repository.*;
import com.bank.sv.AdminStatsService;
import com.bank.utils.DateUtils;
import com.bank.utils.ExcelUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminStatsServiceImpl implements AdminStatsService {
    @Autowired
    private AdminStatsRepository adminStatsRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AlertRepository alertRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void generateStatisticsForDate(Date date) {
        LocalDateTime localDate = date.toInstant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime().withHour(0).withMinute(0).withSecond(0).withNano(0);

        Optional<AdminStatistics> existingStats = adminStatsRepository.findByDate(localDate);
        if (existingStats.isPresent()) {
            return;
        }

        // Tính toán khoảng thời gian
        LocalDateTime startOfDay = localDate;
        LocalDateTime endOfDay = localDate.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        // Chuyển đổi LocalDateTime thành Date cho các repository hiện tại nếu cần
        Date startDate = Date.from(startOfDay.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
        Date endDate = Date.from(endOfDay.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());

        // Tổng số giao dịch trong ngày
        Long totalTransactions = transactionRepository.countAllTransactionByDate(startDate, endDate);

        // Số tiền giao dịch lớn nhất
        BigDecimal maxAmount = transactionRepository.getHighestAmountOfTransferByDate(startDate, endDate);

        // Số tiền giao dịch trung bình trong ngày
        BigDecimal avgAmount = transactionRepository.getAvgAmountOfTransferByDate(startDate, endDate);

        // Số tiền giao dịch thấp nhất trong ngày
        BigDecimal minAmount = transactionRepository.getMinAmountOfTransferByDate(startDate, endDate);

        // Số khách hàng mới trong ngày
        Long newCustomers = customerRepository.countAllNewCustomersByDate(startDate, endDate);

        // Tổng số khách hàng theo ngày
        Long totalCustomers = customerRepository.countTotalCustomersByDate(endDate);

        // Tổng số tài khoản tiết kiệm được tạo theo ngày
        Long newSavingAccounts = accountRepository.countAllNewCreatedSavingAccountsByDate(startDate, endDate);

        // Kiểm tra nếu không có hoạt động gì trong ngày
        if (totalTransactions == 0 && newCustomers == 0 && newSavingAccounts == 0) {
            LocalDateTime previousDay = localDate.minusDays(1);
            Optional<AdminStatistics> previousStats = adminStatsRepository.findTopByDateBeforeOrderByDateDesc(previousDay);

            if (previousStats.isPresent() && previousStats.get().getTotalCustomers().equals(totalCustomers)) {
                return;
            }
        }

        AdminStatistics statistics = AdminStatistics.builder().date(localDate).totalTransactions(totalTransactions).maxTransactionAmount(maxAmount).avgTransactionAmount(avgAmount).minTransactionAmount(minAmount).newCustomers(newCustomers).totalCustomers(totalCustomers).newSavingAccounts(newSavingAccounts).build();

        adminStatsRepository.save(statistics);
    }

    @Override
    @Cacheable(value = "admin_date_stats", key = "{#startDate, #endDate}")
    public List<AdminStatistics> getStatisticsForDateRange(Date startDate, Date endDate) {
        LocalDateTime start = startDate.toInstant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = endDate.toInstant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        return adminStatsRepository.findByDateRange(start, end);
    }

    @Override
    public AdminStatistics getStatisticsForDate(Date date) {
        LocalDateTime localDate = date.toInstant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime().withHour(0).withMinute(0).withSecond(0).withNano(0);

        return adminStatsRepository.findByDate(localDate).orElseThrow(() -> new RuntimeException("Statistic not found"));
    }

    @Override
    public Map<String, Object> getTotalAdminStatForCurrentDate() {
        Long totalTransactions = transactionRepository.countAllTransactions();
        Long totalAlerts = alertRepository.totalAlerts();
        Long totalUsers = adminStatsRepository.countAllCustomers();

        Map<String, Object> result = new HashMap<>();

        result.put("totalTransactions", totalTransactions);
        result.put("totalAlerts", totalAlerts);
        result.put("totalUsers", totalUsers);

        return result;
    }

    @Override
    @Cacheable(value = "admin_week_stats", key = "{#date}")
    public Map<String, Object> getStatisticsForWeek(Date date) {
        //Biến Date thành LocalDate
        LocalDateTime localDate = date.toInstant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime();

        //Lấy ngày đầu tuần và ngày cuối tuần
        LocalDateTime startOfWeek = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfWeek = localDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        //Đổi ngược LocalDate về lại Date
        Date startDate = Date.from(startOfWeek.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
        Date endDate = Date.from(endOfWeek.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());

        List<AdminStatistics> weeklyStats = adminStatsRepository.findByDateRange(startOfWeek, endOfWeek);

        // Tính tổng các giá trị
        Long totalTransactions = 0L;
        BigDecimal maxAmount = BigDecimal.ZERO;
        BigDecimal minAmount = null;
        BigDecimal totalAmount = BigDecimal.ZERO;
        Long totalNewCustomers = 0L;
        Long totalNewSavingAccounts = 0L;

        for (AdminStatistics stat : weeklyStats) {
            totalTransactions += stat.getTotalTransactions();

            if (stat.getMaxTransactionAmount() != null) {
                if (maxAmount.compareTo(stat.getMaxTransactionAmount()) < 0) {
                    maxAmount = stat.getMaxTransactionAmount();
                }
            }

            if (stat.getMinTransactionAmount() != null) {
                if (minAmount == null || minAmount.compareTo(stat.getMinTransactionAmount()) > 0) {
                    minAmount = stat.getMinTransactionAmount();
                }
            }

            if (stat.getAvgTransactionAmount() != null && stat.getTotalTransactions() > 0) {
                totalAmount = totalAmount.add(stat.getAvgTransactionAmount().multiply(new BigDecimal(stat.getTotalTransactions())));
            }

            totalNewCustomers += stat.getNewCustomers();
            totalNewSavingAccounts += stat.getNewSavingAccounts();
        }

        // Tính trung bình
        BigDecimal avgAmount = BigDecimal.ZERO;
        if (totalTransactions > 0) {
            avgAmount = totalAmount.divide(new BigDecimal(totalTransactions), 2, RoundingMode.HALF_UP);
        }

        // Nếu không có dữ liệu thống kê, lấy trực tiếp từ repository
        if (weeklyStats.isEmpty()) {
            totalTransactions = transactionRepository.countAllTransactionByDate(startDate, endDate);
            maxAmount = transactionRepository.getHighestAmountOfTransferByDate(startDate, endDate);
            avgAmount = transactionRepository.getAvgAmountOfTransferByDate(startDate, endDate);
            minAmount = transactionRepository.getMinAmountOfTransferByDate(startDate, endDate);
            totalNewCustomers = customerRepository.countAllNewCustomersByDate(startDate, endDate);
            totalNewSavingAccounts = accountRepository.countAllNewCreatedSavingAccountsByDate(startDate, endDate);
        }

        // Tạo kết quả
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", DateUtils.formatDate(startDate));
        result.put("endDate", DateUtils.formatDate(endDate));
        result.put("totalTransactions", totalTransactions);
        result.put("maxTransactionAmount", maxAmount);
        result.put("avgTransactionAmount", avgAmount);
        result.put("minTransactionAmount", minAmount);
        result.put("totalNewCustomers", totalNewCustomers);
        result.put("totalNewSavingAccounts", totalNewSavingAccounts);

        return result;
    }

    @Override
    @Cacheable(value = "admin_quarter_stats", key = "{#quarter, #year}")
    public Map<String, Object> getStatisticsForQuarter(int quarter, int year) {
        if (quarter < 1 || quarter > 4) {
            throw new RuntimeException(Message.QUARTER_INVALID);
        }

        // Tính toán tháng bắt đầu và kết thúc của quý
        int startMonth = (quarter - 1) * 3 + 1;
        int endMonth = quarter * 3;

        // Ngày bắt đầu và kết thúc
        LocalDateTime startOfQuarter = LocalDateTime.of(year, startMonth, 1, 0, 0, 0);
        LocalDateTime endOfQuarter = LocalDateTime.of(year, endMonth, 1, 23, 59, 59).with(TemporalAdjusters.lastDayOfMonth());

        // Chuyển LocalDate thành Date
        Date startDate = Date.from(startOfQuarter.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
        Date endDate = Date.from(endOfQuarter.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());

        List<AdminStatistics> quarterlyStats = adminStatsRepository.findByDateRange(startOfQuarter, endOfQuarter);

        // Tính tổng các giá trị
        Long totalTransactions = 0L;
        BigDecimal maxAmount = BigDecimal.ZERO;
        BigDecimal minAmount = null;
        BigDecimal totalAmount = BigDecimal.ZERO;
        Long totalNewCustomers = 0L;
        Long totalNewSavingAccounts = 0L;

        for (AdminStatistics stat : quarterlyStats) {
            totalTransactions += stat.getTotalTransactions();

            if (stat.getMaxTransactionAmount() != null) {
                if (maxAmount.compareTo(stat.getMaxTransactionAmount()) < 0) {
                    maxAmount = stat.getMaxTransactionAmount();
                }
            }

            if (stat.getMinTransactionAmount() != null) {
                if (minAmount == null || minAmount.compareTo(stat.getMinTransactionAmount()) > 0) {
                    minAmount = stat.getMinTransactionAmount();
                }
            }

            if (stat.getAvgTransactionAmount() != null && stat.getTotalTransactions() > 0) {
                totalAmount = totalAmount.add(stat.getAvgTransactionAmount().multiply(new BigDecimal(stat.getTotalTransactions())));
            }

            totalNewCustomers += stat.getNewCustomers();
            totalNewSavingAccounts += stat.getNewSavingAccounts();
        }

        // Tính trung bình
        BigDecimal avgAmount = BigDecimal.ZERO;
        if (totalTransactions > 0) {
            avgAmount = totalAmount.divide(new BigDecimal(totalTransactions), 2, RoundingMode.HALF_UP);
        }

        // Nếu không có dữ liệu thống kê, lấy trực tiếp từ repository
        if (quarterlyStats.isEmpty()) {
            totalTransactions = transactionRepository.countAllTransactionByDate(startDate, endDate);
            maxAmount = transactionRepository.getHighestAmountOfTransferByDate(startDate, endDate);
            avgAmount = transactionRepository.getAvgAmountOfTransferByDate(startDate, endDate);
            minAmount = transactionRepository.getMinAmountOfTransferByDate(startDate, endDate);
            totalNewCustomers = customerRepository.countAllNewCustomersByDate(startDate, endDate);
            totalNewSavingAccounts = accountRepository.countAllNewCreatedSavingAccountsByDate(startDate, endDate);
        }

        // Tạo kết quả
        Map<String, Object> result = new HashMap<>();
        result.put("quarter", quarter);
        result.put("year", year);
        result.put("startDate", DateUtils.formatDate(startDate));
        result.put("endDate", DateUtils.formatDate(endDate));
        result.put("totalTransactions", totalTransactions);
        result.put("maxTransactionAmount", maxAmount);
        result.put("avgTransactionAmount", avgAmount);
        result.put("minTransactionAmount", minAmount);
        result.put("totalNewCustomers", totalNewCustomers);
        result.put("totalNewSavingAccounts", totalNewSavingAccounts);

        return result;
    }

    @Override
    @Cacheable(value = "admin_year_stats", key = "{#year}")
    public Map<String, Object> getStatisticsForYear(int year) {
        // Ngày bắt đầu, kết thúc của năm
        LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59);

        // Chuyển LocalDate thành Date
        Date startDate = Date.from(startOfYear.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
        Date endDate = Date.from(endOfYear.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());

        List<AdminStatistics> yearlyStats = adminStatsRepository.findByDateRange(startOfYear, endOfYear);

        // Tính tổng các giá trị
        Long totalTransactions = 0L;
        BigDecimal maxAmount = BigDecimal.ZERO;
        BigDecimal minAmount = null;
        BigDecimal totalAmount = BigDecimal.ZERO;
        Long totalNewCustomers = 0L;
        Long totalNewSavingAccounts = 0L;

        for (AdminStatistics stat : yearlyStats) {
            totalTransactions += stat.getTotalTransactions();

            if (stat.getMaxTransactionAmount() != null) {
                if (maxAmount.compareTo(stat.getMaxTransactionAmount()) < 0) {
                    maxAmount = stat.getMaxTransactionAmount();
                }
            }

            if (stat.getMinTransactionAmount() != null) {
                if (minAmount == null || minAmount.compareTo(stat.getMinTransactionAmount()) > 0) {
                    minAmount = stat.getMinTransactionAmount();
                }
            }

            if (stat.getAvgTransactionAmount() != null && stat.getTotalTransactions() > 0) {
                totalAmount = totalAmount.add(stat.getAvgTransactionAmount().multiply(new BigDecimal(stat.getTotalTransactions())));
            }

            totalNewCustomers += stat.getNewCustomers();
            totalNewSavingAccounts += stat.getNewSavingAccounts();
        }

        // Tính trung bình
        BigDecimal avgAmount = BigDecimal.ZERO;
        if (totalTransactions > 0) {
            avgAmount = totalAmount.divide(new BigDecimal(totalTransactions), 2, RoundingMode.HALF_UP);
        }

        // Nếu không có dữ liệu thống kê, lấy trực tiếp từ repository
        if (yearlyStats.isEmpty()) {
            totalTransactions = transactionRepository.countAllTransactionByDate(startDate, endDate);
            maxAmount = transactionRepository.getHighestAmountOfTransferByDate(startDate, endDate);
            avgAmount = transactionRepository.getAvgAmountOfTransferByDate(startDate, endDate);
            minAmount = transactionRepository.getMinAmountOfTransferByDate(startDate, endDate);
            totalNewCustomers = customerRepository.countAllNewCustomersByDate(startDate, endDate);
            totalNewSavingAccounts = accountRepository.countAllNewCreatedSavingAccountsByDate(startDate, endDate);
        }

        // Tạo kết quả
        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("startDate", DateUtils.formatDate(startDate));
        result.put("endDate", DateUtils.formatDate(endDate));
        result.put("totalTransactions", totalTransactions);
        result.put("maxTransactionAmount", maxAmount);
        result.put("avgTransactionAmount", avgAmount);
        result.put("minTransactionAmount", minAmount);
        result.put("totalNewCustomers", totalNewCustomers);
        result.put("totalNewSavingAccounts", totalNewSavingAccounts);

        return result;
    }

    @Override
    public byte[] exportDailyStatsToExcel(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            throw new RuntimeException(Message.START_END_REQUIRED);
        }

        if (startDate.after(endDate)) {
            throw new RuntimeException(Message.YEAR_REQUIRED);
        }

        LocalDateTime start = startDate.toInstant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = endDate.toInstant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime().withHour(23).withMinute(59).withSecond(59);

        List<AdminStatistics> statsList = adminStatsRepository.findByDateRange(start, end);

        if (statsList.isEmpty()) {
            throw new RuntimeException(Message.DATA_NOT_FOUND);
        }

        // Format period string for report title
        String period = DateUtils.formatDate(startDate) + " - " + DateUtils.formatDate(endDate);

        return ExcelUtils.createAdminStatsExcel(statsList, period);
    }

    @Override
    public byte[] exportWeeklyStatsToExcel(int year) {

        List<AdminStatistics> allYearStats = adminStatsRepository.findAllByYear(year);

        if (allYearStats.isEmpty()) {
            throw new RuntimeException(Message.DATA_NOT_FOUND + year);
        }

        // Map thống kê theo tuần
        Map<Integer, List<AdminStatistics>> weeklyStats = allYearStats.stream()
                .collect(Collectors.groupingBy(stats -> {
                    LocalDateTime date = stats.getDate();
                    return date.get(WeekFields.of(Locale.getDefault()).weekOfYear());
                }));

        List<AdminStatistics> weeklyAggregatedStats = new ArrayList<>();

        for (Map.Entry<Integer, List<AdminStatistics>> entry : weeklyStats.entrySet()) {
            List<AdminStatistics> weekStats = entry.getValue();

            if (weekStats.isEmpty()) continue;

            // ngày đầu tuần
            AdminStatistics firstDayOfWeek = weekStats.get(0);

            Long totalTransactions = weekStats.stream().mapToLong(AdminStatistics::getTotalTransactions).sum();

            BigDecimal maxAmount = weekStats.stream()
                    .map(AdminStatistics::getMaxTransactionAmount)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal minAmount = weekStats.stream()
                    .map(AdminStatistics::getMinTransactionAmount)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal totalWeightedAvg = BigDecimal.ZERO;
            Long totalWeightedCount = 0L;

            for (AdminStatistics stats : weekStats) {
                if (stats.getAvgTransactionAmount() != null && stats.getTotalTransactions() > 0) {
                    totalWeightedAvg = totalWeightedAvg.add(
                            stats.getAvgTransactionAmount().multiply(new BigDecimal(stats.getTotalTransactions())));
                    totalWeightedCount += stats.getTotalTransactions();
                }
            }

            BigDecimal avgAmount = totalWeightedCount > 0 ?
                    totalWeightedAvg.divide(new BigDecimal(totalWeightedCount), 2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            Long newCustomers = weekStats.stream().mapToLong(AdminStatistics::getNewCustomers).sum();
            Long newSavingAccounts = weekStats.stream().mapToLong(AdminStatistics::getNewSavingAccounts).sum();

            Long totalCustomers = weekStats.stream()
                    .max(Comparator.comparing(AdminStatistics::getDate))
                    .map(AdminStatistics::getTotalCustomers)
                    .orElse(0L);

            // Tạo đối tượng thống kê tuần
            AdminStatistics response = AdminStatistics.builder()
                    .date(firstDayOfWeek.getDate())
                    .totalTransactions(totalTransactions)
                    .maxTransactionAmount(maxAmount)
                    .avgTransactionAmount(avgAmount)
                    .minTransactionAmount(minAmount)
                    .newCustomers(newCustomers)
                    .totalCustomers(totalCustomers)
                    .newSavingAccounts(newSavingAccounts)
                    .build();

            weeklyAggregatedStats.add(response);
        }

        weeklyAggregatedStats.sort(Comparator.comparing(AdminStatistics::getDate));

        String period = "Weekly Report " + year;
        return ExcelUtils.createAdminStatsExcel(weeklyAggregatedStats, period);
    }

    @Override
    public byte[] exportQuarterlyStatsToExcel(int year) {
        List<AdminStatistics> allYearStats = adminStatsRepository.findAllByYear(year);

        if (allYearStats.isEmpty()) {
            throw new RuntimeException(Message.DATA_NOT_FOUND + year);
        }

        // Nhóm thống kê theo quý
        Map<Integer, List<AdminStatistics>> quarterlyStats = allYearStats.stream()
                .collect(Collectors.groupingBy(stats -> {
                    int month = stats.getDate().getMonthValue();
                    return (month - 1) / 3 + 1; // Chuyển đổi tháng thành quý (1-4)
                }));

        // Tạo danh sách thống kê quý với giá trị tổng hợp
        List<AdminStatistics> quarterlyAggregatedStats = new ArrayList<>();

        for (int quarter = 1; quarter <= 4; quarter++) {
            List<AdminStatistics> quarterStats = quarterlyStats.getOrDefault(quarter, Collections.emptyList());

            if (quarterStats.isEmpty()) continue;

            // Tính ngày đầu quý
            int startMonth = (quarter - 1) * 3 + 1;
            LocalDateTime quarterStartDate = LocalDateTime.of(year, startMonth, 1, 0, 0, 0);

            // Tính tổng các giá trị
            Long totalTransactions = quarterStats.stream().mapToLong(AdminStatistics::getTotalTransactions).sum();

            BigDecimal maxAmount = quarterStats.stream()
                    .map(AdminStatistics::getMaxTransactionAmount)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal minAmount = quarterStats.stream()
                    .map(AdminStatistics::getMinTransactionAmount)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            // Tính trung bình có trọng số
            BigDecimal totalWeightedAvg = BigDecimal.ZERO;
            Long totalWeightedCount = 0L;

            for (AdminStatistics stats : quarterStats) {
                if (stats.getAvgTransactionAmount() != null && stats.getTotalTransactions() > 0) {
                    totalWeightedAvg = totalWeightedAvg.add(
                            stats.getAvgTransactionAmount().multiply(new BigDecimal(stats.getTotalTransactions())));
                    totalWeightedCount += stats.getTotalTransactions();
                }
            }

            BigDecimal avgAmount = totalWeightedCount > 0 ?
                    totalWeightedAvg.divide(new BigDecimal(totalWeightedCount), 2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            Long newCustomers = quarterStats.stream().mapToLong(AdminStatistics::getNewCustomers).sum();
            Long newSavingAccounts = quarterStats.stream().mapToLong(AdminStatistics::getNewSavingAccounts).sum();

            // Lấy tổng số khách hàng từ ngày cuối cùng của quý
            Long totalCustomers = quarterStats.stream()
                    .max(Comparator.comparing(AdminStatistics::getDate))
                    .map(AdminStatistics::getTotalCustomers)
                    .orElse(0L);

            // Tạo đối tượng thống kê quý
            AdminStatistics quarterlyStatsResponse = AdminStatistics.builder()
                    .date(quarterStartDate)
                    .totalTransactions(totalTransactions)
                    .maxTransactionAmount(maxAmount)
                    .avgTransactionAmount(avgAmount)
                    .minTransactionAmount(minAmount)
                    .newCustomers(newCustomers)
                    .totalCustomers(totalCustomers)
                    .newSavingAccounts(newSavingAccounts)
                    .build();

            quarterlyAggregatedStats.add(quarterlyStatsResponse);
        }

        // Sắp xếp theo thứ tự thời gian
        quarterlyAggregatedStats.sort(Comparator.comparing(AdminStatistics::getDate));

        String period = "Quarterly Report " + year;
        return ExcelUtils.createAdminStatsExcel(quarterlyAggregatedStats, period);
    }

    @Override
    public byte[] exportYearlyStatsToExcel(int year) {

        List<AdminStatistics> yearStats = adminStatsRepository.findAllByYear(year);

        if (yearStats.isEmpty()) {
            throw new RuntimeException(Message.DATA_NOT_FOUND);
        }

        LocalDateTime yearStartDate = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        Long totalTransactions = yearStats.stream().mapToLong(AdminStatistics::getTotalTransactions).sum();

        BigDecimal maxAmount = yearStats.stream()
                .map(AdminStatistics::getMaxTransactionAmount)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal minAmount = yearStats.stream()
                .map(AdminStatistics::getMinTransactionAmount)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalWeightedAvg = BigDecimal.ZERO;
        Long totalWeightedCount = 0L;

        for (AdminStatistics stats : yearStats) {
            if (stats.getAvgTransactionAmount() != null && stats.getTotalTransactions() > 0) {
                totalWeightedAvg = totalWeightedAvg.add(
                        stats.getAvgTransactionAmount().multiply(new BigDecimal(stats.getTotalTransactions())));
                totalWeightedCount += stats.getTotalTransactions();
            }
        }

        BigDecimal avgAmount = totalWeightedCount > 0 ?
                totalWeightedAvg.divide(new BigDecimal(totalWeightedCount), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        Long newCustomers = yearStats.stream().mapToLong(AdminStatistics::getNewCustomers).sum();
        Long newSavingAccounts = yearStats.stream().mapToLong(AdminStatistics::getNewSavingAccounts).sum();

        // Lấy tổng số khách hàng từ ngày cuối cùng của năm
        Long totalCustomers = yearStats.stream()
                .max(Comparator.comparing(AdminStatistics::getDate))
                .map(AdminStatistics::getTotalCustomers)
                .orElse(0L);

        // Tạo đối tượng thống kê năm
        AdminStatistics yearlyStats = AdminStatistics.builder()
                .date(yearStartDate)
                .totalTransactions(totalTransactions)
                .maxTransactionAmount(maxAmount)
                .avgTransactionAmount(avgAmount)
                .minTransactionAmount(minAmount)
                .newCustomers(newCustomers)
                .totalCustomers(totalCustomers)
                .newSavingAccounts(newSavingAccounts)
                .build();

        List<AdminStatistics> yearlyAggregatedStats = new ArrayList<>();
        yearlyAggregatedStats.add(yearlyStats);

        String period = "Yearly Report " + year;
        return ExcelUtils.createAdminStatsExcel(yearlyAggregatedStats, period);
    }
}
