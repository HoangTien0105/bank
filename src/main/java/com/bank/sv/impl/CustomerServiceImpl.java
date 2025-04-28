package com.bank.sv.impl;

import com.bank.constant.Message;
import com.bank.dto.CustomerDto;
import com.bank.dto.PaginDto;
import com.bank.dto.request.CustomerAccountRequestDto;
import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;
import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.model.CustomerType;
import com.bank.repository.AccountRepository;
import com.bank.repository.CustomerRepository;
import com.bank.repository.CustomerTypeRepository;
import com.bank.sv.CustomerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.ValidationException;
import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
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
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PaginDto<CustomerDto> getCustomers(PaginDto<CustomerDto> pagin) {

        // Init các biến bắt đầu và giới hạn phần tử trong trang

        int offset = pagin.getOffset() != null ? pagin.getOffset() : 0;
        int limit = pagin.getLimit() != null ? pagin.getLimit() : 10;

        //Nếu có filter
        String keyword = pagin.getKeyword();

        int pageNumber = offset / limit;
        Pageable pageable = PageRequest.of(pageNumber, limit);

        //Sử dụng criteria để xây dựng truy vấn thay cho sql
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Customer> query = cb.createQuery(Customer.class);
        Root<Customer> root = query.from(Customer.class);

        //Search condition
        List<Predicate> predicates = new ArrayList<>();
        if(StringUtils.hasText(keyword)){
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), searchPattern),
                    cb.like(cb.lower(root.get("phone")), searchPattern),
                    cb.like(cb.lower(root.get("email")), searchPattern)
            ));
        }

        query.where(predicates.toArray(new Predicate[0]));

        //Lấy customers
        List<Customer> customers = entityManager.createQuery(query)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        //Tạo truy vấn đếm số dòng
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Customer> countRoot = countQuery.from(Customer.class);
        //Lấy data chi tiết => SELECT COUNT(*)
        countQuery.select(cb.count(countRoot));
        countQuery.where(predicates.toArray(new Predicate[0]));
        Long totalRows = entityManager.createQuery(countQuery).getSingleResult();

        List<CustomerDto> response = customers.stream()
                .map(customer -> CustomerDto.build(customer, customer.getType()))
                .collect(Collectors.toList());

        pagin.setResults(response);
        pagin.setOffset(offset);
        pagin.setLimit(limit);
        pagin.setPageNumber(pageNumber + 1);
        pagin.setTotalRows(totalRows);
        pagin.setTotalPages((int) Math.ceil((double) totalRows / limit));

        return pagin;
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
            throw new ValidationException("Password are not matches");
        }

        if(customerRepository.existsByEmail(request.getEmail()) != null){
            throw new DuplicateKeyException("Email already existed");
        }

        if(customerRepository.existsByCitizenId(request.getCitizenId()) != null){
            throw new DuplicateKeyException("Citizen ID already existed");
        }

        if(customerRepository.existsByPhone(request.getPhone()) != null){
            throw new DuplicateKeyException("Phone already existed");
        }

        CustomerType customerType = customerTypeRepository.existsByName(request.getCustomerType());

        if(customerType == null)
            throw new ValidationException("Invalid customer's type");

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
                .status(AccountStatus.PENDING)
                .balance(BigDecimal.ZERO)
                .customer(customer)
                .build();

        accountRepository.save(account);
    }
}
