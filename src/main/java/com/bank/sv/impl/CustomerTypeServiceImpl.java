package com.bank.sv.impl;

import com.bank.model.CustomerType;
import com.bank.repository.CustomerTypeRepository;
import com.bank.sv.CustomerTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerTypeServiceImpl implements CustomerTypeService {

    @Autowired
    private CustomerTypeRepository customerTypeRepository;

    @Override
    public void createCustomerType(String name) {
        if (name.trim().isEmpty())
            throw new IllegalArgumentException("Name can't be whitespace");

        if (customerTypeRepository.existsByName(name) != null)
            throw new IllegalArgumentException("Customer type with name " + name + " already existed");
        
        CustomerType customerType = CustomerType.builder()
                .name(name)
                .build();
        customerTypeRepository.save(customerType);
    }
}
