package com.bank.repository;

import com.bank.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface TokenRepository extends JpaRepository<Token, String> {
    void deleteByCustomerId(String customerId);
    Token findByRefreshToken(String refreshToken);
}
