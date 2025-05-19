package com.bank.sv.impl;

import com.bank.constant.Message;
import com.bank.dto.PaginDto;
import com.bank.dto.request.SavingAccountRequestDto;
import com.bank.dto.request.UpdateAccountStatusRequestDto;
import com.bank.dto.response.AccountResponseDto;
import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;
import com.bank.enums.BalanceType;
import com.bank.enums.TransactionType;
import com.bank.model.*;
import com.bank.repository.*;
import com.bank.sv.AccountService;
import com.bank.utils.BalanceTypeUtils;
import com.bank.utils.DateUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountStatusHistoryRepository accountStatusHistoryRepository;

    @Autowired
    private InterestRateConfigRepository interestRateConfigRepository;

    @PersistenceContext
    private EntityManager entityManager;

    //Status cho phép thay đổi
    private static final Map<AccountStatus, Set<AccountStatus>> allowStatusTransaction = Map.of(
            AccountStatus.ACTIVE, Set.of(AccountStatus.INACTIVE, AccountStatus.SUSPENDED),
            AccountStatus.INACTIVE, Set.of(AccountStatus.ACTIVE),
            AccountStatus.SUSPENDED, Set.of(AccountStatus.ACTIVE, AccountStatus.CLOSED)
    );

    @Override
    @Transactional
    public void updateAccountStatus(String id, UpdateAccountStatusRequestDto request) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new RuntimeException(Message.ACCOUNT_NOT_FOUND));

        // Validate và chuyển đổi status từ String sang AccountStatus
        AccountStatus newStatus;
        try {
            newStatus = AccountStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(Message.INVALID_ACCOUNT_STATUS);
        }

        if (account.getStatus() == newStatus) {
            throw new ValidationException(Message.ACCOUNT_STATUS_ALREADY_SET);
        }

        account.setStatus(newStatus);
        accountRepository.save(account);

        AccountStatusHistory history = AccountStatusHistory.builder()
                .status(account.getStatus())
                .reason(request.getReason())
                .customer(account.getCustomer())
                .build();

        accountStatusHistoryRepository.save(history);
    }

    @Override
    @Cacheable(value = "accounts", key = "#paginDto.toString() + '_' + #customerId + '_' + #role")
    public PaginDto<AccountResponseDto> getAccounts(PaginDto<AccountResponseDto> paginDto, String customerId, String role) {

        int offset = paginDto.getOffset() != null ? paginDto.getOffset() : 0;
        int limit = paginDto.getLimit() != null ? paginDto.getLimit() : 10;

        String keyword = paginDto.getKeyword();

        int pageNumber = offset / limit;

        String jpql;
        TypedQuery<Account> query;

        if ("ADMIN".equals(role)) {
            // Admin can see all accounts
            jpql = "SELECT a FROM Account a WHERE " +
                    "(:keyword IS NULL OR " +
                    "LOWER(a.status) LIKE :searchPattern OR " +
                    "LOWER(a.type) LIKE :searchPattern)";

            query = entityManager.createQuery(jpql, Account.class);
        } else {
            // Regular customers can only see their own accounts
            jpql = "SELECT a FROM Account a JOIN a.customer c WHERE " +
                    "c.id = :customerId AND " +
                    "(:keyword IS NULL OR " +
                    "LOWER(a.status) LIKE :searchPattern OR " +
                    "LOWER(a.type) LIKE :searchPattern)";

            query = entityManager.createQuery(jpql, Account.class);
            query.setParameter("customerId", customerId);
        }

        if (StringUtils.hasText(keyword)) {
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            query.setParameter("keyword", keyword);
            query.setParameter("searchPattern", searchPattern);
        } else {
            query.setParameter("keyword", null);
            query.setParameter("searchPattern", null);
        }

        List<Account> accounts = query
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        String countJpql;
        TypedQuery<Long> countQuery;

        if ("ADMIN".equals(role)) {
            countJpql = "SELECT COUNT(a) FROM Account a WHERE " +
                    "(:keyword IS NULL OR " +
                    "LOWER(a.status) LIKE :searchPattern OR " +
                    "LOWER(a.type) LIKE :searchPattern)";

            countQuery = entityManager.createQuery(countJpql, Long.class);
        } else {
            countJpql = "SELECT COUNT(a) FROM Account a JOIN a.customer c WHERE " +
                    "c.id = :customerId AND " +
                    "(:keyword IS NULL OR " +
                    "LOWER(a.status) LIKE :searchPattern OR " +
                    "LOWER(a.type) LIKE :searchPattern)";

            countQuery = entityManager.createQuery(countJpql, Long.class);
            countQuery.setParameter("customerId", customerId);
        }

        if (StringUtils.hasText(keyword)) {
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            countQuery.setParameter("keyword", keyword);
            countQuery.setParameter("searchPattern", searchPattern);
        } else {
            countQuery.setParameter("keyword", null);
            countQuery.setParameter("searchPattern", null);
        }

        Long totalRows = countQuery.getSingleResult();

        List<AccountResponseDto> response = accounts.stream()
                .map(AccountResponseDto::build)
                .toList();

        paginDto.setResults(response);
        paginDto.setOffset(offset);
        paginDto.setLimit(limit);
        paginDto.setPageNumber(pageNumber + 1);
        paginDto.setTotalRows(totalRows);
        paginDto.setTotalPages((int) Math.ceil((double) totalRows / limit));

        return paginDto;
    }

    @Override
    public AccountResponseDto getAccountById(String id) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new RuntimeException(Message.ACCOUNT_NOT_FOUND));
        return AccountResponseDto.build(account);
    }

    @Override
    public PaginDto<AccountResponseDto> getAccountsGroupByType(PaginDto<AccountResponseDto> paginDto, String customerId, String role) {
        int offset = paginDto.getOffset() != null ? paginDto.getOffset() : 0;
        int limit = paginDto.getLimit() != null ? paginDto.getLimit() : 10;
        String keyword = paginDto.getKeyword();
        int pageNumber = offset / limit;

        String jpql;
        TypedQuery<Account> query;

        if ("ADMIN".equals(role)) {
            // Admin can see all accounts
            jpql = "SELECT a FROM Account a WHERE " +
                    "(:keyword IS NULL OR " +
                    "LOWER(a.status) LIKE :searchPattern OR " +
                    "LOWER(a.type) LIKE :searchPattern) " +
                    "ORDER BY a.type, a.balance DESC";

            query = entityManager.createQuery(jpql, Account.class);
        } else {
            // Regular customers can only see their own accounts
            jpql = "SELECT a FROM Account a JOIN a.customer c WHERE " +
                    "c.id = :customerId AND " +
                    "(:keyword IS NULL OR " +
                    "LOWER(a.status) LIKE :searchPattern OR " +
                    "LOWER(a.type) LIKE :searchPattern) " +
                    "ORDER BY a.type, a.balance DESC";

            query = entityManager.createQuery(jpql, Account.class);
            query.setParameter("customerId", customerId);
        }

        if (StringUtils.hasText(keyword)) {
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            query.setParameter("keyword", keyword);
            query.setParameter("searchPattern", searchPattern);
        } else {
            query.setParameter("keyword", null);
            query.setParameter("searchPattern", null);
        }

        List<Account> accounts = query
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        String countJpql;
        TypedQuery<Long> countQuery;

        if ("ADMIN".equals(role)) {
            countJpql = "SELECT COUNT(a) FROM Account a WHERE " +
                    "(:keyword IS NULL OR " +
                    "LOWER(a.status) LIKE :searchPattern OR " +
                    "LOWER(a.type) LIKE :searchPattern)";

            countQuery = entityManager.createQuery(countJpql, Long.class);
        } else {
            countJpql = "SELECT COUNT(a) FROM Account a JOIN a.customer c WHERE " +
                    "c.id = :customerId AND " +
                    "(:keyword IS NULL OR " +
                    "LOWER(a.status) LIKE :searchPattern OR " +
                    "LOWER(a.type) LIKE :searchPattern)";

            countQuery = entityManager.createQuery(countJpql, Long.class);
            countQuery.setParameter("customerId", customerId);
        }

        if (StringUtils.hasText(keyword)) {
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            countQuery.setParameter("keyword", keyword);
            countQuery.setParameter("searchPattern", searchPattern);
        } else {
            countQuery.setParameter("keyword", null);
            countQuery.setParameter("searchPattern", null);
        }

        Long totalRows = countQuery.getSingleResult();

        List<AccountResponseDto> response = accounts.stream()
                .map(AccountResponseDto::build)
                .toList();

        paginDto.setResults(response);
        paginDto.setOffset(offset);
        paginDto.setLimit(limit);
        paginDto.setPageNumber(pageNumber + 1);
        paginDto.setTotalRows(totalRows);
        paginDto.setTotalPages((int) Math.ceil((double) totalRows / limit));

        return paginDto;
    }

    @Override
    @Transactional
    public void createSavingAccount(SavingAccountRequestDto requestDto, String customerId) {
        //Validate cus
        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> new RuntimeException("Customer not found"));

        //Validate account
        Account sourceAccount = accountRepository.findById(requestDto.getSourceAccountId()).orElseThrow(() -> new RuntimeException("Source account not found"));

        // Validate source accounts có phải của cus k
        if (!sourceAccount.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("Source account does not belong to customer");
        }

        if (sourceAccount.getType() != AccountType.CHECKING || sourceAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Source account must be a checking account and active");
        }

        if (sourceAccount.getBalance().compareTo(requestDto.getAmount()) < 0) {
            throw new RuntimeException("Source account balance is not enough");
        }

        //Lấy các gói tiết kiệm
        InterestRateConfig rateConfig = interestRateConfigRepository.findApplicableRatesByMonths(requestDto.getTermMonths());

        if (rateConfig == null) {
            throw new RuntimeException("No interest rate config match " + requestDto.getTermMonths() + " months");
        }

        int depositDay = 0;
        if (requestDto.getMonthlyDepositAmount() != null && requestDto.getMonthlyDepositAmount().compareTo(BigDecimal.ZERO) > 0) {
            depositDay = LocalDate.now().getDayOfMonth();

            // Nếu ngày tạo từ 29-31, mặc định sẽ nạp vào ngày 1 tháng sau
            if (depositDay >= 29) {
                depositDay = 1;
            }
        }

        //Tính ngày đáo hạn
        Date maturityDate = Date.from(LocalDate.now().plusMonths(requestDto.getTermMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant());

        //Tạo tài khoản tiết kiệm
        Account savingAccount = Account.builder()
                .type(AccountType.SAVING)
                .status(AccountStatus.ACTIVE)
                .customer(customer)
                .balance(requestDto.getAmount())
                .interestRate(rateConfig.getInterestRate())
                .maturityDate(maturityDate)
                .sourceAccount(sourceAccount)
                .savingScheduleDay(depositDay > 0 ? depositDay : null)
                .monthlyDepositAmount(requestDto.getMonthlyDepositAmount())
                .build();

        // Set balance type dựa trên số tiền
        String balanceStatus = BalanceTypeUtils.validateBalanceStatus(savingAccount);
        savingAccount.setBalanceType(BalanceType.valueOf(balanceStatus));
        BalanceTypeUtils.setTransactionLimitBasedOnBalance(savingAccount);

        //Trừ tiền tài khoản gốc
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(requestDto.getAmount()));
        String sourceBalanceStatus = BalanceTypeUtils.validateBalanceStatus(sourceAccount);
        sourceAccount.setBalanceType(BalanceType.valueOf(sourceBalanceStatus));
        BalanceTypeUtils.setTransactionLimitBasedOnBalance(sourceAccount);

        // Tạo transaction cho việc tạo tài khoản tiết kiệm

        Date transactionDate = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());

        Transaction transaction = Transaction.builder()
                .type(TransactionType.SAVING_CREATION)
                .amount(requestDto.getAmount())
                .location("SYSTEM")
                .description("Initial deposit for saving account")
                .account(savingAccount)
                .transactionDate(transactionDate)
                .build();

        //Save
        accountRepository.save(sourceAccount);
        accountRepository.save(savingAccount);
        transactionRepository.save(transaction);
    }

    @Scheduled(cron = "0 * * * * ?") // Chạy vào 00:00:00 mỗi ngày và inactive các tài khoản đến ngày đáo hạn
    @Transactional
    public void processSavingAccount() {
        List<Account> maturedAccounts = accountRepository
                .findByTypeAndStatusAndmaturityDateLessThanEqual(
                        AccountType.SAVING,
                        AccountStatus.ACTIVE,
                        new Date());

        for (Account savingAccount : maturedAccounts) {
            Account sourceAccount = savingAccount.getSourceAccount();
            // Tính lãi
            BigDecimal principal = savingAccount.getBalance();
            BigDecimal interest = calculateInterest(savingAccount);
            BigDecimal totalAmount = principal.add(interest);

            // Tạo giao dịch lãi
            Transaction transaction = Transaction.builder()
                    .type(TransactionType.INTEREST)
                    .amount(totalAmount)
                    .location("SYSTEM")
                    .description("Interest payment for saving account " + savingAccount.getId())
                    .account(sourceAccount)
                    .transactionDate(new Date())
                    .build();

            // Chuyển tiền gốc + lãi
            sourceAccount.setBalance(sourceAccount.getBalance().add(totalAmount));

            //Cập nhật lại balance type
            String sourceBalanceStatus = BalanceTypeUtils.validateBalanceStatus(sourceAccount);
            sourceAccount.setBalanceType(BalanceType.valueOf(sourceBalanceStatus));
            BalanceTypeUtils.setTransactionLimitBasedOnBalance(sourceAccount);

            //Inactive tài khoản tiết kiệm
            savingAccount.setStatus(AccountStatus.INACTIVE);
            savingAccount.setBalance(BigDecimal.ZERO);

            //Save
            accountRepository.save(savingAccount);
            accountRepository.save(sourceAccount);
            transactionRepository.save(transaction);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void monthlyDeposit() {
        int currentDay = LocalDate.now().getDayOfMonth();

        //Tìm tài khoản tiết kiệm có ngày bằng currentDay và số tiền cần nạp > 0
        List<Account> accountsToDeposit = accountRepository
                .findByTypeAndStatusAndSavingScheduleDayAndMonthlyDepositAmountGreaterThan(
                        AccountType.SAVING,
                        AccountStatus.ACTIVE,
                        currentDay,
                        BigDecimal.ZERO
                );

        for (Account account : accountsToDeposit) {
            Account sourceAccount = account.getSourceAccount();

            Transaction transaction = Transaction.builder()
                    .amount(account.getMonthlyDepositAmount())
                    .location("SYSTEM")
                    .account(sourceAccount)
                    .build();

            //Kiểm tra xem tài khoản đủ tiền k
            if (sourceAccount.getBalance().compareTo(account.getMonthlyDepositAmount()) >= 0) {
                //Trừ tiền tài khoản nguồn
                sourceAccount.setBalance(sourceAccount.getBalance().subtract(account.getMonthlyDepositAmount()));

                //Cộng tiền vào tài khoản tiết kiệm
                account.setBalance(account.getBalance().add(account.getMonthlyDepositAmount()));

                //Cập nhật balance type
                String sourceBalanceStatus = BalanceTypeUtils.validateBalanceStatus(sourceAccount);
                sourceAccount.setBalanceType(BalanceType.valueOf(sourceBalanceStatus));
                BalanceTypeUtils.setTransactionLimitBasedOnBalance(sourceAccount);

                String savingBalanceStatus = BalanceTypeUtils.validateBalanceStatus(account);
                account.setBalanceType(BalanceType.valueOf(savingBalanceStatus));
                BalanceTypeUtils.setTransactionLimitBasedOnBalance(account);

                //Tạo transaction success
                transaction.setType(TransactionType.SAVING_DEPOSIT_SUCCESS);
                transaction.setDescription("Monthly deposit to saving account " + account.getId());
                //Save
                accountRepository.save(sourceAccount);
                accountRepository.save(account);
            } else {
                //Tạo transaction failed
                transaction.setType(TransactionType.SAVING_DEPOSIT_FAILED);
                transaction.setDescription("Failed monthly deposit to saving account " + account.getId() + " due to insufficient balance in source account");
            }
            transactionRepository.save(transaction);
        }
    }

    private BigDecimal calculateInterest(Account savingAccount) {
        // Tính số tháng kể từ ngày mở đến ngày đáo hạn
        LocalDate startDate = savingAccount.getCreateDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        LocalDate endDate = savingAccount.getMaturityDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();


        int months = DateUtils.calculateMonths(startDate, endDate);

        //Lãi suất hàng tháng
        BigDecimal monthlyRate = savingAccount.getInterestRate()
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        // Trường hợp có tiền gửi hàng tháng
        // Lấy giao dịch tạo tài khoản để biết số dư ban đầu
        Transaction creationTransaction = transactionRepository.findFirstByAccountAndTypeOrderByTransactionDateAsc(savingAccount, TransactionType.SAVING_CREATION);

        BigDecimal initialBalance = (creationTransaction != null) ? creationTransaction.getAmount() : savingAccount.getBalance();

        // Không có tiền gửi hàng tháng
        if (savingAccount.getMonthlyDepositAmount() == null ||
                savingAccount.getMonthlyDepositAmount().compareTo(BigDecimal.ZERO) <= 0) {

            // Tính lãi kép = Số tiền gốc * [(1 + lãi suất tháng)^số tháng - 1]
            return savingAccount.getBalance().multiply
                    ((BigDecimal.ONE.add(monthlyRate)
                            .pow(months))
                            .subtract(BigDecimal.ONE))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        //Lấy tất cả giao dịch gửi tiền hằng tháng thành công
        List<Transaction> successfulDeposits = transactionRepository.findByAccountAndTypeOrderByTransactionDateAsc(savingAccount, TransactionType.SAVING_DEPOSIT_SUCCESS);

        //Nếu không có giao dịch thì làm như ban đầu
        if(successfulDeposits.isEmpty()){
            return initialBalance.multiply(
                    (BigDecimal.ONE.add(monthlyRate)
                            .pow(months))
                            .subtract(BigDecimal.ONE)
            ).setScale(2, RoundingMode.HALF_UP);
        }

        //Tính lãi kép cho cả số dư ban đầu và các khoản tiền gửi hàng tháng
        BigDecimal totalInterest = BigDecimal.ZERO;

        // 1. Tính lão cho số dư ban đầu từ ngày mở đến ngày đáo hạn
        BigDecimal initialInterest = initialBalance.multiply(
                BigDecimal.ONE.add(monthlyRate)
                        .pow(months)
                        .subtract(BigDecimal.ONE)
        );
        totalInterest = totalInterest.add(initialInterest);

        //2. Tính lãi cho từng khoản tiền gửi hàng tháng
        for (Transaction deposit : successfulDeposits) {
            LocalDate depositDate = deposit.getTransactionDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            // Tính số tháng từ ngày gửi đến ngày đáo hạn
            int remainingMonths = DateUtils.calculateMonths(depositDate, endDate);

            // Tính lãi cho khoản tiền gửi này
            if (remainingMonths > 0) {
                BigDecimal depositInterest = deposit.getAmount().multiply(
                        BigDecimal.ONE.add(monthlyRate)
                                .pow(remainingMonths)
                                .subtract(BigDecimal.ONE)
                );
                totalInterest = totalInterest.add(depositInterest);
            }
        }
        return totalInterest.setScale(2, RoundingMode.HALF_UP);
    }
}
