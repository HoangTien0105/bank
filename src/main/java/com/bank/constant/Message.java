package com.bank.constant;

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
}
