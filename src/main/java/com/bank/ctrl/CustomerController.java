package com.bank.ctrl;

import com.bank.dto.CustomerDto;
import com.bank.dto.PaginDto;
import com.bank.dto.response.ResponseDto;
import com.bank.sv.CustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping
    public ResponseEntity<Object> getCustomers(@RequestParam(value = "offset", defaultValue = "0") String offset,
                                               @RequestParam(value = "limit", defaultValue = "10") String limit,
                                               @RequestParam(value = "keyword", required = false) String keyword){
        PaginDto<CustomerDto> pagin = new PaginDto<>();
        pagin.setOffset(offset);
        pagin.setLimit(limit);
        pagin.setKeyword(keyword);

        PaginDto<CustomerDto> result = customerService.getCustomers(pagin);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getCustomerById(@RequestParam(value = "id") String id){
        CustomerDto customer = customerService.getCustomerById(id);

        return ResponseEntity.ok(customer);
    }

    @GetMapping("/name")
    public ResponseEntity<Object> getCustomerByNAme(@RequestParam(value = "name") String name){
        List<CustomerDto> customerList = customerService.getCustomerByName(name);

        return ResponseEntity.ok(customerList);
    }

}
