package com.bank.constant;

import java.math.BigDecimal;

public interface Value {

    //Transaction Limit and alert
    // Hạn mức giao dịch cho khách hàng cá nhân tạm thời (50 triệu)
    public static final BigDecimal TEMPORARY = new BigDecimal("5000000");
    // Hạn mức giao dịch cho khách hàng cá nhân (50 triệu)
    public static final BigDecimal PERSONAL = new BigDecimal("50000000");
    // Hạn mức giao dịch cho doanh nghiệp (5 tỷ)
    public static final BigDecimal BUSINESS = new BigDecimal("5000000000");

    //Balance
    public static final BigDecimal FIVE_MILLION = new BigDecimal("5000000"); // 5 triệu
    public static final BigDecimal FIFTY_MILLION = new BigDecimal("50000000"); // 50 triệu
    public static final BigDecimal HUNDRED_MILLION = new BigDecimal("100000000"); // 100 triệu

    //CustomerType
    public static final String PERSONAL_TYPE = "PERSONAL";
    public static final String BUSINESS_TYPE = "BUSINESS";
    public static final String TEMPORARY_TYPE = "TEMPORARY";
}
