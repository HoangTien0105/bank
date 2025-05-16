package com.bank.ctrl;

import com.bank.constant.Message;
import com.bank.dto.CustomerDto;
import com.bank.dto.PaginDto;
import com.bank.dto.request.CustomerAccountRequestDto;
import com.bank.dto.request.CustomerUpdateRequestDto;
import com.bank.dto.response.CustomerLocationDto;
import com.bank.dto.response.ResponseDto;
import com.bank.sv.CustomerService;
import com.bank.sv.CustomerTypeService;
import com.bank.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
                                               @RequestParam(value = "keyword", required = false) String keyword){
        PaginDto<CustomerDto> paginDto = new PaginDto<>();
        paginDto.setOffset(offset);
        paginDto.setLimit(limit);
        paginDto.setKeyword(keyword);

        PaginDto<CustomerDto> result = customerService.getCustomers(paginDto);

        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, result));
    }

    @GetMapping("{id}")
    public ResponseEntity<Object> getCustomerById(@RequestParam(value = "id") String id){
        CustomerDto customer = customerService.getCustomerById(id);

        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, customer));
    }

    @GetMapping("/name")
    public ResponseEntity<Object> getCustomerByNAme(@RequestParam(value = "name") String name){
        List<CustomerDto> customerList = customerService.getCustomerByName(name);
        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, customerList));
    }

    @Operation(
            summary = "Create customers with accounts"
    )
    @PostMapping
    public ResponseEntity<Object> createCustomerWithAccount(@Valid @RequestBody(required = true) CustomerAccountRequestDto request){
        try{
            customerService.createCustomer(request);
        } catch (Exception e){
            return ResponseEntity.badRequest().body(apiResponse.response(e.getMessage(), false, null));
        }
        return ResponseEntity.ok(apiResponse.response("Create customer with accounts successfully", true, null));
    }

    @PutMapping("{id}")
    public ResponseEntity<Object> updateCustomerInfo(@PathVariable String id, @Valid @RequestBody(required = true) CustomerUpdateRequestDto request){
        try{
            customerService.updateCustomer(id, request);
        } catch (Exception e){
            return ResponseEntity.badRequest().body(apiResponse.response(e.getMessage(), false, null));
        }
        return ResponseEntity.ok(apiResponse.response("Update successfully", true, null));
    }


    @PostMapping("/customerType")
    public ResponseEntity<Object> createType(@RequestParam(value = "name") String name){
        try{
            customerTypeService.createCustomerType(name);
        } catch (Exception e){
            return ResponseEntity.badRequest().body(apiResponse.response(e.getMessage(), false, null));
        }
        return ResponseEntity.ok(apiResponse.response("Create type successfully", true, null));
    }

    @Operation(
            summary = "Get customers by location"
    )
    @GetMapping("/place")
    public ResponseEntity<Object> getCustomersByLocation(@RequestParam(value = "offset", defaultValue = "0") String offset,
                                                         @RequestParam(value = "limit", defaultValue = "10") String limit,
                                                         @RequestParam(value = "location") String location){
        PaginDto<CustomerLocationDto> paginDto = new PaginDto<>();
        paginDto.setOffset(offset);
        paginDto.setLimit(limit);

        PaginDto<CustomerLocationDto> result = customerService.getCustomersByLocation(paginDto, location);

        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, result));
    }
}
