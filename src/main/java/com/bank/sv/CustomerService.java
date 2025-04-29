package com.bank.sv;

import com.bank.dto.CustomerDto;
import com.bank.dto.PaginDto;
import com.bank.dto.request.CustomerAccountRequestDto;
import com.bank.dto.request.CustomerUpdateRequestDto;

import java.util.List;

public interface CustomerService {
    PaginDto<CustomerDto> getCustomers(PaginDto<CustomerDto> pagin);
    CustomerDto getCustomerById(String id);
    List<CustomerDto> getCustomerByName(String name);
    void createCustomer(CustomerAccountRequestDto request);
    void updateCustomer(String id, CustomerUpdateRequestDto request);
}
