package com.bank.dto.response;

import com.bank.model.Account;
import com.bank.model.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomerWithAccounResponseDto {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String accountId;

    public static CustomerWithAccounResponseDto build(Customer cus, Account account){
        return builder()
                .id(cus.getId())
                .name(cus.getName())
                .email(cus.getEmail())
                .phone(cus.getPhone())
                .accountId(account.getId())
                .build();
    }
}
