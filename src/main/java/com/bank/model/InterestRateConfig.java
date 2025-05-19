package com.bank.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "interest_rate_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestRateConfig extends BaseEntity {

    // Kỳ hạn tính bằng tháng
    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    // Lãi suất theo % hằng năm
    @Column(name = "interest_rate", nullable = false, columnDefinition = "decimal")
    private BigDecimal interestRate;

    @Column(name = "description")
    private String description;

    //Active hay inactive
    @Column(name = "status")
    private Boolean status;
}
