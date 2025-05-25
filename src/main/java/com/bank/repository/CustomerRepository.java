package com.bank.repository;

import com.bank.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
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

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.createDate BETWEEN :startDate AND :endDate")
    Long countAllNewCustomersByDate(Date startDate, Date endDate);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.createDate <= :endDate")
    Long countTotalCustomersByDate(Date endDate);

    @Query("SELECT DISTINCT c FROM Customer c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Customer> findByNameOrPhoneLike(@Param("search") String search);
}
