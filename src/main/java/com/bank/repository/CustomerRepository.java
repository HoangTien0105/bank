package com.bank.repository;

import com.bank.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    @Query("SELECT c FROM Customer c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Customer> findByName(@Param("name") String name);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.email) = LOWER(:email)")
    Customer existsByEmail(@Param("email") String email);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.citizenId) = LOWER(:citizenId)")
    Customer existsByCitizenId(@Param("citizenId") String citizenId);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.phone) = LOWER(:phone)")
    Customer existsByPhone(@Param("phone") String phone);

    @Query("SELECT DISTINCT c FROM Customer " +
           "JOIN Account a on a.customerId = c.id " +
           "JOIN Transaction t on t.accountId = a.id " +
           "WHERE t.location LIKE :location " +
           "ORDER BY t.transaction_date")
    Page<Customer> findCustomerByLocation(@Param("location") String location, Pageable pageable);
}
