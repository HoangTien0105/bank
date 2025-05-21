package com.bank.utils;

import com.bank.constant.Value;
import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.model.CustomerType;

import java.math.BigDecimal;

public class BalanceTypeUtils {

    public static String validateBalanceStatus(Account account) {
        BigDecimal balance = account.getBalance();
        BigDecimal fiveMillion = Value.FIVE_MILLION; // 5 triệu
        BigDecimal fiftyMillion = Value.FIFTY_MILLION; // 50 triệu

        if (balance.compareTo(fiveMillion) < 0) {
            return "LOW";
        } else if (balance.compareTo(fiftyMillion) <= 0) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }
}
