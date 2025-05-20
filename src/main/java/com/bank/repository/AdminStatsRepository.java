package com.bank.repository;

import com.bank.model.AdminStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminStatsRepository extends JpaRepository<AdminStatistics, Long> {
    Optional<AdminStatistics> findByDate(LocalDateTime date);

    @Query("SELECT s FROM AdminStatistics s WHERE s.date BETWEEN :startDate AND :endDate ORDER BY s.date")
    List<AdminStatistics> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
