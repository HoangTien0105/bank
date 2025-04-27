package com.bank.sv;

import com.bank.dto.CustomerDto;
import com.bank.dto.PaginDto;

import java.util.List;

public interface CustomerService {
    PaginDto<CustomerDto> getCustomers(PaginDto<CustomerDto> pagin);
    CustomerDto getCustomerById(String id);
    List<CustomerDto> getCustomerByName(String name);
}
