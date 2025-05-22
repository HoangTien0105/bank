package com.bank.repository;

import com.bank.model.CustomerStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerStatsRepository extends JpaRepository<CustomerStatistics, Long> {
    Optional<CustomerStatistics> findByYearAndMonthAndCustomerId(Integer year, Integer month, String customerId);

    @Query("SELECT cs FROM CustomerStatistics cs WHERE cs.customer.id = :customerId AND "
            + "((cs.year = :startYear AND cs.month >= :startMonth) OR (cs.year > :startYear)) AND "
            + "((cs.year = :endYear AND cs.month <= :endMonth) OR (cs.year < :endYear)) "
            + "ORDER BY cs.year, cs.month")
    List<CustomerStatistics> findByCustomerIdAndDateRange(
            @Param("customerId") String customerId,
            @Param("startYear") Integer startYear,
            @Param("startMonth") Integer startMonth,
            @Param("endYear") Integer endYear,
            @Param("endMonth") Integer endMonth);

    @Query("SELECT cs FROM CustomerStatistics cs WHERE "
            + "((cs.year = :year AND cs.month = :month))")
    List<CustomerStatistics> findAllByYearAndMonth(
            @Param("year") Integer year,
            @Param("month") Integer month);

    @Query("SELECT cs FROM CustomerStatistics cs WHERE "
            + "cs.year = :year")
    List<CustomerStatistics> findAllByYear(
            @Param("year") Integer year);

    @Query("SELECT cs FROM CustomerStatistics cs WHERE cs.year = :year AND cs.customer.id = :customerId")
    List<CustomerStatistics> findByYearAndCustomerId(@Param("year") Integer year, @Param("customerId") String customerId);
}
