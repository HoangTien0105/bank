package com.bank.ctrl;

import com.bank.dto.PaginDto;
import com.bank.dto.request.MoneyTransferRequestDto;
import com.bank.dto.request.MoneyUpdateRequest;
import com.bank.dto.response.TransactionResponseDto;
import com.bank.model.JwtUser;
import com.bank.sv.TransactionService;
import com.bank.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions")
public class TransactionController {
    @Autowired
    private APIResponse apiResponse;

    @Autowired
    private TransactionService transactionService;

    @GetMapping
    public ResponseEntity<Object> getTransactions(@RequestParam(value = "offset", defaultValue = "0") Integer offset,
                                                  @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                                                  @RequestParam(value = "keyword", required = false) String keyword,
                                                  @RequestParam(value = "location", required = false) String location,
                                                  @RequestParam(value = "minAmount", required = false) Double minAmount,
                                                  @RequestParam(value = "maxAmount", required = false) Double maxAmount,
                                                  @RequestParam(value = "sortBy", required = false) String sortBy,
                                                  @RequestParam(value = "sortDirection", required = false, defaultValue = "ASC") String sortDirection) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();

        PaginDto<TransactionResponseDto> paginDto = new PaginDto<>();
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

        if (minAmount != null) {
            options.put("minAmount", minAmount);
        }
        if (maxAmount != null) {
            options.put("maxAmount", maxAmount);
        }

        paginDto.setOptions(options);

        PaginDto<TransactionResponseDto> result = transactionService.getTransactions(paginDto, jwtUser.getId(), jwtUser.getRole());

        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, result));
    }

    @GetMapping("{id}")
    public ResponseEntity<Object> getTransactionByID(@PathVariable(value = "id") String id) {
        TransactionResponseDto response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, response));
    }

    @Operation(summary = "Transfer money")
    @PostMapping(value = "/transfer")
    public ResponseEntity<Object> transferMoney(@Valid @RequestBody MoneyTransferRequestDto request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        TransactionResponseDto transaction = transactionService.transferMoney(request, jwtUser.getId());

        return ResponseEntity.ok(apiResponse.response("Transfer money successfully", true, transaction));
    }

    @Operation(summary = "Deposit money")
    @PostMapping(value = "/deposit")
    public ResponseEntity<Object> depositMoney(@Valid @RequestBody MoneyUpdateRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        TransactionResponseDto transaction = transactionService.depositMoney(request, jwtUser.getId());

        return ResponseEntity.ok(apiResponse.response("Deposit money successfully", true, transaction));
    }

    @Operation(summary = "Withdraw money")
    @PostMapping(value = "/withdraw")
    public ResponseEntity<Object> withdrawMoney(@Valid @RequestBody MoneyUpdateRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();
        TransactionResponseDto transaction = transactionService.withdrawMoney(request, jwtUser.getId());
        return ResponseEntity.ok(apiResponse.response("Withdraw money successfully", true, transaction));
    }

    @Operation(summary = "Get transactions by date")
    @GetMapping(value = "/date")
    public ResponseEntity<Object> getTransactionsByDate(@RequestParam(value = "offset", defaultValue = "0") String offset,
                                                        @RequestParam(value = "limit", defaultValue = "10") String limit) {
        PaginDto<TransactionResponseDto> paginDto = new PaginDto<>();
        paginDto.setOffset(offset);
        paginDto.setLimit(limit);

        PaginDto<TransactionResponseDto> result = transactionService.getTransactionsOrderByDate(paginDto);

        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, result));
    }

    @Operation(summary = "Get transactions by account id")
    @GetMapping(value = "/account/{id}")
    public ResponseEntity<Object> getTransactionsByAccountId(@PathVariable(value = "id") String id,
                                                             @RequestParam(value = "offset", defaultValue = "0") String offset,
                                                             @RequestParam(value = "limit", defaultValue = "10") String limit) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();

        PaginDto<TransactionResponseDto> paginDto = new PaginDto<>();
        paginDto.setOffset(offset);
        paginDto.setLimit(limit);

        Map<String, Object> options = new HashMap<>();

        options.put("accountId", id);

        paginDto.setOptions(options);

        PaginDto<TransactionResponseDto> result = transactionService.getTransactions(paginDto, jwtUser.getId(), jwtUser.getRole());

        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, result));
    }
}
