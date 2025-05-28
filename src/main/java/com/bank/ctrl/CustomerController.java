package com.bank.ctrl;

import com.bank.constant.Message;
import com.bank.constant.Value;
import com.bank.dto.CustomerDto;
import com.bank.dto.PaginDto;
import com.bank.dto.request.CustomerAccountRequestDto;
import com.bank.dto.request.CustomerUpdateRequestDto;
import com.bank.dto.response.CustomerLocationDto;
import com.bank.dto.response.CustomerWithAccounResponseDto;
import com.bank.model.JwtUser;
import com.bank.sv.CustomerService;
import com.bank.sv.CustomerTypeService;
import com.bank.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers")
public class CustomerController {

    @Autowired
    private APIResponse apiResponse;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerTypeService customerTypeService;

    @GetMapping
    public ResponseEntity<Object> getCustomers(@RequestParam(value = "offset", defaultValue = "0") String offset,
                                               @RequestParam(value = "limit", defaultValue = "10") String limit,
                                               @RequestParam(value = "keyword", required = false) String keyword,
                                               @RequestParam(value = "location", required = false) String location,
                                               @RequestParam(value = "sortBy", required = false) String sortBy,
                                               @RequestParam(value = "sortDirection", required = false, defaultValue = "ASC") String sortDirection) {
        PaginDto<CustomerDto> paginDto = new PaginDto<>();
        paginDto.setOffset(offset);
        paginDto.setLimit(limit);
        paginDto.setKeyword(keyword);

        Map<String, Object> options = new HashMap<>();
        if (sortBy != null) {
            options.put("sortBy", sortBy);
            options.put("sortDirection", sortDirection);
        }

        if (location != null) {
            options.put("location", location);
        }

        paginDto.setOptions(options);

        PaginDto<CustomerDto> result = customerService.getCustomers(paginDto);

        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, result));
    }

    @GetMapping("{id}")
    public ResponseEntity<Object> getCustomerById(@RequestParam(value = "id") String id) {
        CustomerDto customer = customerService.getCustomerById(id);

        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, customer));
    }

    @GetMapping("/name")
    public ResponseEntity<Object> getCustomerByNAme(@RequestParam(value = "name") String name) {
        List<CustomerDto> customerList = customerService.getCustomerByName(name);
        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, customerList));
    }

    @Operation(
            summary = "Create customers with accounts"
    )
    @PostMapping
    public ResponseEntity<Object> createCustomerWithAccount(@Valid @RequestBody(required = true) CustomerAccountRequestDto request) {
        customerService.createCustomer(request);
        return ResponseEntity.ok(apiResponse.response("Create customer with accounts successfully", true, null));
    }

    @PutMapping("{id}")
    public ResponseEntity<Object> updateCustomerInfo(@PathVariable String id, @Valid @RequestBody(required = true) CustomerUpdateRequestDto request) {
        customerService.updateCustomer(id, request);
        return ResponseEntity.ok(apiResponse.response("Update successfully", true, null));
    }


    @PostMapping("/customerType")
    public ResponseEntity<Object> createType(@RequestParam(value = "name") String name) {
        customerTypeService.createCustomerType(name);
        return ResponseEntity.ok(apiResponse.response("Create type successfully", true, null));
    }

    @Operation(summary = "Search customer by name, phone or account id")
    @GetMapping("/search")
    public ResponseEntity<Object> searchCustomer(@RequestParam(value = "search") String search, Authentication authentication) {
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        List<CustomerWithAccounResponseDto> customer = customerService.searchCustomer(search, jwtUser.getId());
        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, customer));
    }
}
