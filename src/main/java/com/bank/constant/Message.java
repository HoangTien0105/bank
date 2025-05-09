package com.bank.constant;

import lombok.Data;


public interface Message {

    //Validate Customer
    String CUS_NOT_FOUND = "Customer not found!";
    String DUPLICATED_EMAIL = "Email already existed";
    String DUPLICATED_PHONE = "Phone already existed";
    String DUPLICATED_CITIZEN = "Citizen ID already existed";
    String INVALID_PASSWORD = "Passwords are not match";
    String INVALID_CUS_TYPE = "Invalid customer's type";

    String UPDATE_FAIL = "Update failed";
    String CREATE_FAIL = "Create failed";

    //Validate Account
    String ACCOUNT_NOT_FOUND = "Account not found!";
    String INVALID_ACCOUNT_STATUS = "Invalid account status";
    String ACCOUNT_STATUS_ALREADY_SET = "Account's status is already set";

    //Validate transaction
    String TRANSACTION_NOT_FOUND = "Transaction not found!";
    String TRANSFER_FAILED = "Transfer money failed";
    String TRANSFER_SUCCESS = "Transfer money successfully";
}
