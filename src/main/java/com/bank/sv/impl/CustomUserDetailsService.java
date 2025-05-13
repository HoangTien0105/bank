package com.bank.sv.impl;

import com.bank.model.Customer;
import com.bank.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private CustomerRepository customerRepository;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.role}")
    private String adminRole;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //Kiểm tra phải admin
        if(username.equals(adminUsername)){
            return new User(
                    adminUsername,
                    adminPassword,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + adminRole))
            );
        }

        //Login theo mail và sđt
        Customer customer = customerRepository.findByEmailOrPhone(username);
        if(customer == null){
            throw new UsernameNotFoundException("Customer not found with email/phone: " + username);
        }

        return new User(
                customer.getId(),
                customer.getPassword(), // Giả sử customer có trường password đã mã hóa
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }
}
