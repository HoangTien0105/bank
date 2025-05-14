package com.bank.repository;

import com.bank.model.InterestRateConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterestRateConfigRepository extends JpaRepository<InterestRateConfig, String> {

    @Query("SELECT i FROM InterestRateConfig i WHERE i.status = true AND i.termMonths <= :months ORDER BY i.termMonths DESC")
    List<InterestRateConfig> findApplicableRates(@Param("months") Long months);

    @Query("SELECT i FROM InterestRateConfig i WHERE i.status = true AND i.termMonths = :months ORDER BY i.termMonths DESC")
    InterestRateConfig findApplicableRatesByMonths(@Param("months") Long months);

    @Query("SELECT i FROM InterestRateConfig i WHERE i.status = true ORDER BY i.termMonths ASC")
    List<InterestRateConfig> findAllActiveRates();
}
