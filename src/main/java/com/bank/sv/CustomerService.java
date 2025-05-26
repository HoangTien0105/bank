package com.bank.sv;

import com.bank.dto.CustomerDto;
import com.bank.dto.PaginDto;
import com.bank.dto.request.CustomerAccountRequestDto;
import com.bank.dto.request.CustomerUpdateRequestDto;
import com.bank.dto.request.RegisterRequestDto;
import com.bank.dto.response.CustomerLocationDto;
import com.bank.dto.response.CustomerWithAccounResponseDto;

import java.util.List;

public interface CustomerService {
    PaginDto<CustomerDto> getCustomers(PaginDto<CustomerDto> paginDto);
    void register(RegisterRequestDto registerRequestDto);
    CustomerDto getCustomerById(String id);
    List<CustomerDto> getCustomerByName(String name);
    List<CustomerWithAccounResponseDto> searchCustomer(String search, String cusId);
    void createCustomer(CustomerAccountRequestDto request);
    void updateCustomer(String id, CustomerUpdateRequestDto request);
    PaginDto<CustomerLocationDto> getCustomersByLocation(PaginDto<CustomerLocationDto> paginDto, String location);
}
