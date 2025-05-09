package com.bank.utils;

import com.bank.model.Account;

import java.math.BigDecimal;

public class BalanceTypeUtils {
    public static String validateBalanceStatus(Account account) {
        BigDecimal balance = account.getBalance();
        BigDecimal fiveMillion = new BigDecimal("5000000"); // 5 triệu
        BigDecimal fiftyMillion = new BigDecimal("50000000"); // 50 triệu

        if (balance.compareTo(fiveMillion) < 0) {
            return "LOW";
        } else if (balance.compareTo(fiftyMillion) <= 0) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }

    public static void setTransactionLimitBasedOnBalance(Account account) {
        BigDecimal fiveMillion = new BigDecimal("5000000"); // 5 triệu
        BigDecimal fiftyMillion = new BigDecimal("50000000"); // 50 triệu
        BigDecimal hundredMillion = new BigDecimal("100000000"); // 100 triệu

        String status = validateBalanceStatus(account);
        switch (status) {
            case "LOW":
                account.setTransactionLimit(fiveMillion);
                break;
            case "MEDIUM":
                account.setTransactionLimit(fiftyMillion);
                break;
            case "HIGH":
                account.setTransactionLimit(hundredMillion);
                break;
        }
    }
}
