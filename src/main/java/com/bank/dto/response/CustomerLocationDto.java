package com.bank.dto.response;

import com.bank.dto.CustomerDto;
import com.bank.model.Customer;
import com.bank.model.CustomerType;
import com.bank.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomerLocationDto {
    private String id;
    private String name;
    private String email;
    private String citizenId;
    private String phone;
    private String address;
    private String password;
    private Date createDate;
    private String customerType;
    private String location;

    public static CustomerLocationDto build(Customer cus, CustomerType cusType, Transaction transaction){
        return builder()
                .id(cus.getId())
                .name(cus.getName())
                .email(cus.getEmail())
                .citizenId(cus.getCitizenId())
                .phone(cus.getPhone())
                .address(cus.getAddress())
                .createDate(cus.getCreateDate())
                .customerType(cusType.getName())
                .location(transaction.getLocation())
                .build();
    }
}
