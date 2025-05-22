package com.bank.sv;

import com.bank.dto.response.CustomerStatsResponseDto;

import java.util.Date;
import java.util.List;

public interface CustomerStatsService {
    void generateStatsForMonth(Integer year, Integer month);
    List<CustomerStatsResponseDto> getStatsForCustomer(String cusId, Date startDate, Date endDate);
    List<CustomerStatsResponseDto> getStatsForMonth(Integer year, Integer month);
    byte[] exportToExcel(Integer year, Integer month);
    byte[] exportWeeklyStatsToExcel(String customerId, int year);
    byte[] exportQuarterlyStatsToExcel(String customerId, int year);
    byte[] exportYearlyStatsToExcel(String customerId, int year);
}
