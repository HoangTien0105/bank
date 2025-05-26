package com.bank.sv;

import com.bank.dto.request.InterestRateRequestDto;
import com.bank.model.InterestRateConfig;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface InterestRateConfigService {
    List<InterestRateConfig> getAllActiveRates();
    void createRate(InterestRateRequestDto requestDto);
    void updateRate(String id, InterestRateRequestDto requestDto);
    void updateRateStatus(String id);
    //Trả về lãi suất dựa vào số ngày
    BigDecimal getApplicableRate(Date startDate, Date maturityDate);
    //Trả về số tiền dự tính sẽ nhận đựợc
    BigDecimal calculateInterest(Double principal, Integer months, Double annualRate, Double monthlyDeposit);
}
