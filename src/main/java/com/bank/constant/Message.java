package com.bank.constant;

import lombok.Data;


public interface Message {

    //Data not found
    String DATA_NOT_FOUND = "Data not found";

    //Validate Customer
    String CUS_ID_REQUIRED = "Customer ID is required";
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
    String TRANSFER_INTEREST = "Transfer money from interest ";

    //Validate token
    String TOKEN_NOT_FOUND = "Token not found!";

    //Validate alert
    String ALERT_NOT_FOUND = "Alert not found";
    String ALERT_STATUS_REQUIRED = "Alert status is required";
    String ALERT_STATUS_INVALID = "Invalid alert status";

    //Validate time
    String START_BEFORE_END = "Start year must be before end year";
    String YEAR_REQUIRED = "Year is required";
    String START_END_REQUIRED = "Start date and end date are required";
    String QUARTER_INVALID = "Quarter must be between 1 and 4";

}
