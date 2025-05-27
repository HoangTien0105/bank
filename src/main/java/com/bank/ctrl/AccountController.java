package com.bank.ctrl;

import com.bank.dto.request.SavingAccountRequestDto;
import com.bank.dto.response.AccountResponseDto;
import com.bank.dto.PaginDto;
import com.bank.dto.request.UpdateAccountStatusRequestDto;
import com.bank.model.JwtUser;
import com.bank.sv.AccountService;
import com.bank.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private APIResponse apiResponse;

    @GetMapping
    public ResponseEntity<Object> getAccounts(@RequestParam(value = "offset", defaultValue = "0") Integer offset,
                                              @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                                              @RequestParam(value = "keyword", required = false) String keyword,
                                              @RequestParam(value = "balanceType", required = false) String balanceType,
                                              @RequestParam(value = "accountType", required = false) String accountType,
                                              @RequestParam(value = "sortBy", required = false) String sortBy,
                                              @RequestParam(value = "sortDirection", required = false, defaultValue = "ASC") String sortDirection) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();

        PaginDto<AccountResponseDto> paginDto = new PaginDto<>();
        paginDto.setOffset(offset);
        paginDto.setLimit(limit);
        paginDto.setKeyword(keyword);

        Map<String, Object> options = new HashMap<>();
        if (sortBy != null) {
            options.put("sortBy", sortBy);
            options.put("sortDirection", sortDirection);
        }

        if (balanceType != null) {
            options.put("balanceType", balanceType);
        }

        if (accountType != null) {
            options.put("accountType", accountType);
        }

        paginDto.setOptions(options);


        PaginDto<AccountResponseDto> result = accountService.getAccounts(paginDto, jwtUser.getId(), jwtUser.getRole());

        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, result));
    }

    @GetMapping("{id}")
    public ResponseEntity<Object> getAccountById(@PathVariable(value = "id") String id) {
        AccountResponseDto response = accountService.getAccountById(id);
        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, response));
    }

    @Operation(
            summary = "Update account status"
    )
    @PutMapping("{id}/status")
    public ResponseEntity<Object> updateAccountStatus(@PathVariable("id") String id, @Valid @RequestBody UpdateAccountStatusRequestDto request) {
        try {
            accountService.updateAccountStatus(id, request);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(apiResponse.response("Update account successfully", true, null));
        }
        return ResponseEntity.ok(apiResponse.response("Update account successfully", true, null));
    }

    @Operation(
            summary = "Get accounts group by type"
    )
    @GetMapping("/type")
    public ResponseEntity<Object> getAccountsGroupByType(@RequestParam(value = "offset", defaultValue = "0") Integer offset,
                                                         @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        PaginDto<AccountResponseDto> paginDto = new PaginDto<>();
        paginDto.setOffset(offset);
        paginDto.setLimit(limit);

        PaginDto<AccountResponseDto> result = accountService.getAccountsGroupByType(paginDto, jwtUser.getId(), jwtUser.getRole());

        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, result));
    }

    @Operation(summary = "Create saving account")
    @PostMapping("/saving")
    public ResponseEntity<Object> createSavingAccount(
            @Valid @RequestBody SavingAccountRequestDto requestDto,
            Authentication authentication
    ) {
        try {
            JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
            AccountResponseDto account = accountService.createSavingAccount(requestDto, jwtUser.getId());
            return ResponseEntity.ok(apiResponse.response("Create saving account successfully", true, account));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(apiResponse.response(e.getMessage(), false, null));
        }
    }

    @Operation(summary = "Get tracking account by customer id")
    @GetMapping("/customer")
    public ResponseEntity<Object> getTrackingAccountByCustomerId(
            Authentication authentication
    ) {
        try {
            JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
            AccountResponseDto account = accountService.getTrackingAccountByCusId(jwtUser.getId());
            return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, account));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(apiResponse.response(e.getMessage(), false, null));
        }
    }
}
