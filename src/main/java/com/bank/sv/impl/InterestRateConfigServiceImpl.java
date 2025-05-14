package com.bank.sv.impl;

import com.bank.dto.request.InterestRateRequestDto;
import com.bank.model.InterestRateConfig;
import com.bank.repository.InterestRateConfigRepository;
import com.bank.sv.InterestRateConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class InterestRateConfigServiceImpl implements InterestRateConfigService {

    @Autowired
    private InterestRateConfigRepository interestRateConfigRepository;

    @Override
    public List<InterestRateConfig> getAllActiveRates() {
        return interestRateConfigRepository.findAllActiveRates();
    }

    @Override
    @Transactional
    public void createRate(InterestRateRequestDto requestDto) {
        List<InterestRateConfig> existRates = interestRateConfigRepository.findAllActiveRates();
        boolean duplicatedTerms = existRates.stream().anyMatch(rate -> rate .getTermMonths().equals(requestDto.getTermMonths()));

        if(duplicatedTerms){
            throw new RuntimeException("Term with " + requestDto.getTermMonths() + " months already existed");
        }

        InterestRateConfig config = InterestRateConfig.builder()
                .termMonths(requestDto.getTermMonths())
                .interestRate(BigDecimal.valueOf(requestDto.getRate()))
                .description(requestDto.getDescription())
                .status(false)
                .build();

        interestRateConfigRepository.save(config);
    }

    @Override
    @Transactional
    public void updateRate(String id, InterestRateRequestDto requestDto) {
        InterestRateConfig config = interestRateConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Config not found"));

        // Nếu thay đổi kỳ hạn, kiểm tra trùng lặp
        if (!config.getTermMonths().equals(requestDto.getTermMonths())) {
            List<InterestRateConfig> existingRates = interestRateConfigRepository.findAllActiveRates();
            boolean duplicateTerm = existingRates.stream()
                    .filter(rate -> !rate.getId().equals(id)) // Loại trừ config hiện tại
                    .anyMatch(rate -> rate.getTermMonths().equals(requestDto.getTermMonths()));

            if (duplicateTerm) {
                throw new RuntimeException("Term with " + requestDto.getTermMonths() + " months already existed");
            }
        }

        config.setTermMonths(requestDto.getTermMonths());
        config.setInterestRate(BigDecimal.valueOf(requestDto.getRate()));
        config.setDescription(requestDto.getDescription());

        interestRateConfigRepository.save(config);
    }

    @Override
    @Transactional
    public void updateRateStatus(String id) {
        InterestRateConfig config = interestRateConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        config.setStatus(!config.getStatus());
        interestRateConfigRepository.save(config);
    }

    @Override
    public BigDecimal getApplicableRate(Date startDate, Date maturityDate) {
        // Tính số tháng giữa ngày bắt đầu và ngày đáo hạn
        Long months = ChronoUnit.MONTHS.between(
                startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                maturityDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        );

        List<InterestRateConfig> rates = interestRateConfigRepository.findApplicableRates(months);

        if (rates.isEmpty()) {
            throw new RuntimeException("No subscriptions found within " + months + " months");
        }

        //Trả về cái có lãi suất cao nhất để thuyết phục khách hàng
        return rates.get(0).getInterestRate();
    }
}
