package com.bank.sv.impl;

import com.bank.model.AdminStatistics;
import com.bank.repository.AccountRepository;
import com.bank.repository.AdminStatsRepository;
import com.bank.repository.CustomerRepository;
import com.bank.repository.TransactionRepository;
import com.bank.sv.AdminStatsService;
import com.bank.utils.DateUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

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

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void generateStatisticsForDate(Date date) {
        // Chuyển đổi Date thành LocalDateTime
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
        BigDecimal minAmount = transactionRepository.getAvgAmountOfTransferByDate(startDate, endDate);

        // Số khách hàng mới trong ngày
        Long newCustomers = customerRepository.countAllNewCustomersByDate(startDate, endDate);

        // Tổng số khách hàng theo ngày
        Long totalCustomers = customerRepository.countTotalCustomersByDate(endDate);

        // Tổng số tài khoản tiết kiệm được tạo theo ngày
        Long newSavingAccounts = accountRepository.countAllNewCreatedSavingAccountsByDate(startDate, endDate);

        // Kiểm tra nếu không có hoạt động gì trong ngày
        if (totalTransactions == 0 && newCustomers == 0 && newSavingAccounts == 0) {
            // Lấy thống kê của ngày gần nhất trước đó
            LocalDateTime previousDay = localDate.minusDays(1);
            Optional<AdminStatistics> previousStats = adminStatsRepository.findByDate(previousDay);

            if (previousStats.isPresent() && previousStats.get().getTotalCustomers().equals(totalCustomers)) {
                return;
            }
        }

        AdminStatistics statistics = AdminStatistics.builder().date(localDate).totalTransactions(totalTransactions).maxTransactionAmount(maxAmount).avgTransactionAmount(avgAmount).minTransactionAmount(minAmount).newCustomers(newCustomers).totalCustomers(totalCustomers).newSavingAccounts(newSavingAccounts).build();

        adminStatsRepository.save(statistics);
    }

    @Override
    public List<AdminStatistics> getStatisticsForDateRange(Date startDate, Date endDate) {
        // Chuyển đổi Date thành LocalDateTime
        LocalDateTime start = startDate.toInstant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime().withHour(0).withMinute(0).withSecond(0).withNano(0);

        LocalDateTime end = endDate.toInstant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime().withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        return adminStatsRepository.findByDateRange(start, end);
    }

    @Override
    public AdminStatistics getStatisticsForDate(Date date) {
        // Chuyển đổi Date thành LocalDateTime
        LocalDateTime localDate = date.toInstant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime().withHour(0).withMinute(0).withSecond(0).withNano(0);

        return adminStatsRepository.findByDate(localDate).orElseThrow(() -> new RuntimeException("Statistic not found"));
    }

    @Override
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
    public Map<String, Object> getStatisticsForQuarter(int quarter, int year) {
        if (quarter < 1 || quarter > 4) {
            throw new IllegalArgumentException("Quarter must be between 1 and 4");
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
}
