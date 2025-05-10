package com.bank.sv.impl;

import com.bank.constant.Message;
import com.bank.dto.CustomerDto;
import com.bank.dto.PaginDto;
import com.bank.dto.request.CustomerAccountRequestDto;
import com.bank.dto.request.CustomerUpdateRequestDto;
import com.bank.dto.response.CustomerLocationDto;
import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;
import com.bank.enums.BalanceType;
import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.model.CustomerType;
import com.bank.repository.AccountRepository;
import com.bank.repository.CustomerRepository;
import com.bank.repository.CustomerTypeRepository;
import com.bank.repository.TransactionRepository;
import com.bank.sv.CustomerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerTypeRepository customerTypeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PaginDto<CustomerDto> getCustomers(PaginDto<CustomerDto> paginDto) {

        // Init các biến bắt đầu và giới hạn phần tử trong trang

        int offset = paginDto.getOffset() != null ? paginDto.getOffset() : 0;
        int limit = paginDto.getLimit() != null ? paginDto.getLimit() : 10;

        //Nếu có filter
        String keyword = paginDto.getKeyword();

        int pageNumber = offset / limit;

        String jpql = "SELECT c FROM Customer c WHERE " +
                "(:keyword IS NULL OR " +
                "LOWER(c.name) LIKE :searchPattern OR " +
                "LOWER(c.phone) LIKE :searchPattern OR " +
                "LOWER(c.email) LIKE :searchPattern)";


        TypedQuery<Customer> query = entityManager.createQuery(jpql, Customer.class);

        //Lấy customers
        if (StringUtils.hasText(keyword)) {
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            query.setParameter("keyword", keyword);
            query.setParameter("searchPattern", searchPattern);
        } else {
            query.setParameter("keyword", null);
            query.setParameter("searchPattern", null);
        }

        List<Customer> customers = query
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        //Tạo truy vấn đếm số dòng
        String countJpql = "SELECT COUNT(c) FROM Customer c WHERE " +
                "(:keyword IS NULL OR " +
                "LOWER(c.name) LIKE :searchPattern OR " +
                "LOWER(c.phone) LIKE :searchPattern OR " +
                "LOWER(c.email) LIKE :searchPattern)";
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        if (StringUtils.hasText(keyword)) {
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            countQuery.setParameter("keyword", keyword);
            countQuery.setParameter("searchPattern", searchPattern);
        } else {
            countQuery.setParameter("keyword", null);
            countQuery.setParameter("searchPattern", null);
        }

        Long totalRows = countQuery.getSingleResult();

        List<CustomerDto> response = customers.stream()
                .map(customer -> CustomerDto.build(customer, customer.getType()))
                .collect(Collectors.toList());

        paginDto.setResults(response);
        paginDto.setOffset(offset);
        paginDto.setLimit(limit);
        paginDto.setPageNumber(pageNumber + 1);
        paginDto.setTotalRows(totalRows);
        paginDto.setTotalPages((int) Math.ceil((double) totalRows / limit));

        return paginDto;
    }

    @Override
    public CustomerDto getCustomerById(String id) {
        Customer customer = customerRepository.findById(id).orElseThrow(() -> new RuntimeException(Message.CUS_NOT_FOUND));
        return CustomerDto.build(customer, customer.getType());
    }

    @Override
    public List<CustomerDto> getCustomerByName(String name) {
        List<Customer> customerList = customerRepository.findByName(name);

        return customerList.stream().map(customer -> CustomerDto.build(customer, customer.getType())).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createCustomer(CustomerAccountRequestDto request) {
        if(!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException(Message.INVALID_PASSWORD);
        }

        if(customerRepository.existsByEmail(request.getEmail()) != null){
            throw new DuplicateKeyException(Message.DUPLICATED_EMAIL);
        }

        if(customerRepository.existsByCitizenId(request.getCitizenId()) != null){
            throw new DuplicateKeyException(Message.DUPLICATED_CITIZEN);
        }

        if(customerRepository.existsByPhone(request.getPhone()) != null){
            throw new DuplicateKeyException(Message.DUPLICATED_PHONE);
        }

        CustomerType customerType = customerTypeRepository.existsByName(request.getCustomerType());

        if(customerType == null)
            throw new ValidationException(Message.INVALID_CUS_TYPE);

        Customer customer = Customer.builder()
                .name(request.getName())
                .email(request.getEmail())
                .citizenId(request.getCitizenId())
                .phone(request.getPhone())
                .address(request.getAddress())
                .password(passwordEncoder.encode(request.getPassword()))
                .type(customerType)
                .build();

        customerRepository.save(customer);

        Account account = Account.builder()
                .type(AccountType.CHECKING)
                .balanceType(BalanceType.LOW)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .transactionLimit(BigDecimal.valueOf(5000000))
                .customer(customer)
                .build();

        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void updateCustomer(String id, CustomerUpdateRequestDto request) {
        Customer customer = customerRepository.findById(id).orElseThrow(() -> new RuntimeException(Message.CUS_NOT_FOUND));

        if(StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(customer.getEmail())){
            if(customerRepository.existsByEmail(request.getEmail()) != null){
                throw new DuplicateKeyException(Message.DUPLICATED_EMAIL);
            }
            customer.setEmail(request.getEmail());
        }

        if(StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(customer.getPhone())){
            if(customerRepository.existsByPhone(request.getPhone()) != null){
                throw new DuplicateKeyException(Message.DUPLICATED_PHONE);
            }
            customer.setPhone(request.getPhone());
        }

        if(StringUtils.hasText(request.getCitizenId()) && !request.getCitizenId().equals(customer.getCitizenId())){
            if(customerRepository.existsByCitizenId(request.getCitizenId()) != null){
                throw new DuplicateKeyException(Message.DUPLICATED_CITIZEN);
            }
            customer.setCitizenId(request.getCitizenId());
        }

        if(StringUtils.hasText(request.getAddress())){
            customer.setAddress(request.getAddress());
        }

        if(StringUtils.hasText(request.getCustomerType())){

            CustomerType customerType = customerTypeRepository.existsByName(request.getCustomerType());
            if(customerType == null){
                throw new ValidationException(Message.INVALID_CUS_TYPE);
            }
            customer.setType(customerType);
        }

        customerRepository.save(customer);
    }

    @Override
    public PaginDto<CustomerLocationDto> getCustomersByLocation(PaginDto<CustomerLocationDto> paginDto, String location) {
        int offset = paginDto.getOffset() != null ? paginDto.getOffset() : 0;
        int limit = paginDto.getLimit() != null ? paginDto.getLimit() : 10;

        int pageNumber = offset/limit;

        //Tạo pageable
        Pageable pageable = PageRequest.of(pageNumber, limit);

        Page<Object[]> customers =  customerRepository.findCustomerByLocation("%" + location + "%", pageable);
        List<CustomerLocationDto> result = customers.getContent().stream()
                .map(row -> {

            // Tạo CustomerLocationDto
                    String id = row[0].toString();         // c.id (numeric -> String)
                    String name = (String) row[1];         // c.name
                    String email = (String) row[2];        // c.email
                    String citizenId = (String) row[3];    // c.citizen_id
                    String phone = (String) row[4];        // c.phone
                    String address = (String) row[5];      // c.address
                    String customerType = (String) row[6]; // ct.name
                    String transactionLocation = (String) row[7]; // t.location

                    // Tạo CustomerLocationDto
                    return CustomerLocationDto.builder()
                            .id(id)
                            .name(name)
                            .email(email)
                            .citizenId(citizenId)
                            .phone(phone)
                            .address(address)
                            .customerType(customerType)
                            .location(transactionLocation)
                            .build();
                })
                .collect(Collectors.toList());

        PaginDto<CustomerLocationDto> response = new PaginDto<>();
        response.setOffset(offset);
        response.setLimit(limit);
        response.setTotalPages(customers.getTotalPages());
        response.setTotalRows(customers.getTotalElements());
        response.setResults(result);

        return response;
    }
}
