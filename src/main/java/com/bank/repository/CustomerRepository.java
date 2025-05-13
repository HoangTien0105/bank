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

    @Query(value = "SELECT DISTINCT c.id, c.name, c.email, c.citizen_id, c.phone, c.address," +
            "ct.name AS customer_type_name, t.location, t.transaction_date " +
            "FROM customer c " +
            "INNER JOIN customer_type ct ON c.type_id = ct.id " +
            "INNER JOIN account a ON c.id = a.customer_id " +
            "INNER JOIN transaction t ON a.id = t.account_id " +
            "WHERE t.location LIKE :location " +
            "ORDER BY t.transaction_date DESC",
            countQuery = "SELECT COUNT(DISTINCT c.id) " +
                    "FROM customer c " +
                    "INNER JOIN account a ON c.id = a.customer_id " +
                    "INNER JOIN transaction t ON a.id = t.account_id " +
                    "WHERE t.location LIKE :location",
            nativeQuery = true)
    Page<Object[]> findCustomerByLocation(@Param("location") String location, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.email) = LOWER(:username) OR LOWER(c.phone) = LOWER(:username)")
    Customer findByEmailOrPhone(@Param("username") String username);
}
