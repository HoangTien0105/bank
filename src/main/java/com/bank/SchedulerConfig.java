package com.bank;

import com.bank.model.AdminStatistics;
import com.bank.sv.AccountService;
import com.bank.sv.AdminStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Date;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Autowired
    private AdminStatsService adminStatsService;

    @Autowired
    private AccountService accountService;

    // Chạy vào 00:05 mỗi ngày
    @Scheduled(cron = "0 5 0 * * ?")
    public void generateDailyStatistics() {
        // Tạo thống kê cho ngày hôm trước
        Date yesterday = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        adminStatsService.generateStatisticsForDate(yesterday);
    }

    // Chạy vào 00:00 mỗi ngày để xử lý tài khoản tiết kiệm đến hạn
    @Scheduled(cron = "0 0 0 * * ?")
    public void processSavingAccounts() {
        accountService.processSavingAccount();
    }

    // Chạy vào 00:00 mỗi ngày để xử lý nạp tiền hàng tháng
    @Scheduled(cron = "0 0 0 * * ?")
    public void processMonthlyDeposits() {
        accountService.monthlyDeposit();
    }
}
