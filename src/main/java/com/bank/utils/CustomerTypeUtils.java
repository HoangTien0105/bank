package com.bank.utils;

import com.bank.constant.Value;
import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.model.CustomerType;

import java.math.BigDecimal;

public class CustomerTypeUtils {

    public static void setTransactionLimitBasedOnCustomerType(Account account) {
        Customer customer = account.getCustomer();
        if (customer != null && customer.getType() != null) {
            CustomerType customerType = customer.getType();
            String customerTypeName = customerType.getName();

            if (Value.BUSINESS_TYPE.equalsIgnoreCase(customerTypeName)) {
                account.setTransactionLimit(Value.BUSINESS);
            } else {
                // Mặc định là khách hàng cá nhân
                account.setTransactionLimit(Value.PERSONAL);
            }
        } else {
            // Nếu không có thông tin khách hàng, mặc định là khách hàng cá nhân
            account.setTransactionLimit(Value.PERSONAL);
        }
    }

    // Lấy ngưỡng cảnh báo dựa trên loại khách hàng
    public static BigDecimal getAlertThresholdByCustomerType(CustomerType customerType) {
        if (customerType != null) {
            String customerTypeName = customerType.getName();
            if (Value.BUSINESS_TYPE.equalsIgnoreCase(customerTypeName)) {
                return Value.BUSINESS;
            }
        }
        // Mặc định là khách hàng cá nhân
        return Value.PERSONAL;
    }
}
