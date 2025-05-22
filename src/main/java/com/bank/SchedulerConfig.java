package com.bank;

import com.bank.model.AdminStatistics;
import com.bank.sv.AccountService;
import com.bank.sv.AdminStatsService;
import com.bank.sv.AlertService;
import com.bank.sv.CustomerStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Autowired
    private AdminStatsService adminStatsService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private CustomerStatsService customerStatsService;

    // Chạy vào 00:00 mỗi ngày
    @Scheduled(cron = "0 0 0 * * ?")
    public void generateDailyStatistics() {
        // Tạo thống kê cho ngày hôm trước
        Date yesterday = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        CompletableFuture.runAsync(() -> { //	Chạy task bất đồng bộ không trả kết quả
            try {
                adminStatsService.generateStatisticsForDate(yesterday);
            } catch (Exception e) {
                // Log lỗi
                System.err.println("Error generating statistics: " + e.getMessage());
            }
        });
    }

    // Chạy vào 00:00 mỗi ngày để xử lý tài khoản tiết kiệm đến hạn
    @Scheduled(cron = "0 0 0 * * ?")
    public void processSavingAccounts() {
        CompletableFuture.runAsync(() -> {
            try {
                accountService.processSavingAccount();
            } catch (Exception e) {
                // Log lỗi
                System.err.println("Error processing saving accounts: " + e.getMessage());
            }
        });
    }

    // Chạy vào 00:00 mỗi ngày để xử lý nạp tiền hàng tháng
    @Scheduled(cron = "0 0 0 * * ?")
    public void processMonthlyDeposits() {
        CompletableFuture.runAsync(() -> {
            try {
                accountService.monthlyDeposit();
            } catch (Exception e) {
                // Log lỗi
                System.err.println("Error processing monthly deposits: " + e.getMessage());
            }
        });
    }

    // Thêm lịch trình kiểm tra giao dịch bất thường mỗi 1 tiếng
    @Scheduled(cron = "0 0 * * * ?")
    public void detectAbnormalTransactions() {
        CompletableFuture.runAsync(() -> {
            try {
                alertService.detectAbnormalTransactions();
            } catch (Exception e) {
                // Log lỗi
                System.err.println("Error detecting abnormal transactions: " + e.getMessage());
            }
        });
    }

    @Scheduled(cron = "0 0 0 1 * ?")
    public void generateMonthlyCustomerStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastMonth = now.minusMonths(1);
        int year = lastMonth.getYear();
        int month = lastMonth.getMonthValue();
        CompletableFuture.runAsync(() -> {
            try {
                customerStatsService.generateStatsForMonth(year, month);
            } catch (Exception e) {
                // Log lỗi
                System.err.println("Error generating monthly customer stats: " + e.getMessage());
            }
        });
    }
}
