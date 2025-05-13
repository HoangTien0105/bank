package com.bank.repository;

import com.bank.model.CustomerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerTypeRepository extends JpaRepository<CustomerType, Long> {
    @Query("SELECT ct FROM CustomerType ct WHERE LOWER(ct.name) = LOWER(:name)")
    CustomerType existsByName(@Param(value = "name") String name);
}
