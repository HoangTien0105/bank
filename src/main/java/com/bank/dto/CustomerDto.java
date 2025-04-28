package com.bank.dto;

import com.bank.model.Customer;
import com.bank.model.CustomerType;
import lombok.*;

import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomerDto {
    private String id;
    private String name;
    private String email;
    private String citizenId;
    private String phone;
    private String address;
    private String password;
    private Date createDate;
    private String customerType;

    public static CustomerDto build(Customer cus, CustomerType cusType){
        return builder()
                .id(cus.getId())
                .name(cus.getName())
                .email(cus.getEmail())
                .citizenId(cus.getCitizenId())
                .phone(cus.getPhone())
                .address(cus.getAddress())
                .createDate(cus.getCreateDate())
                .customerType(cusType.getName())
                .build();
    }
}
