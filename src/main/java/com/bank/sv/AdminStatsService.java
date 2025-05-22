package com.bank.sv;

import com.bank.model.AdminStatistics;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface AdminStatsService {
    void generateStatisticsForDate(Date date);
    List<AdminStatistics> getStatisticsForDateRange(Date startDate, Date endDate);
    AdminStatistics getStatisticsForDate(Date date);

    Map<String, Object> getStatisticsForWeek(Date date);
    Map<String, Object> getStatisticsForQuarter(int quarter, int year);
    Map<String, Object> getStatisticsForYear(int year);

    //Excel export
    byte[] exportDailyStatsToExcel(Date startDate, Date endDate);
    byte[] exportWeeklyStatsToExcel(int year);
    byte[] exportQuarterlyStatsToExcel(int year);
    byte[] exportYearlyStatsToExcel(int year);
}
