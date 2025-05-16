package com.bank.ctrl;

import com.bank.dto.PaginDto;
import com.bank.dto.request.MoneyTransferRequestDto;
import com.bank.dto.request.MoneyUpdateRequest;
import com.bank.dto.response.ResponseDto;
import com.bank.dto.response.TransactionResponseDto;
import com.bank.model.JwtUser;
import com.bank.sv.TransactionService;
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
                                              @RequestParam(value = "keyword", required = false) String keyword){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtUser jwtUser = (JwtUser) authentication.getPrincipal();

        PaginDto<TransactionResponseDto> paginDto = new PaginDto<>();
        paginDto.setOffset(offset);
        paginDto.setLimit(limit);
        paginDto.setKeyword(keyword);

        PaginDto<TransactionResponseDto> result = transactionService.getTransactions(paginDto, jwtUser.getId(), jwtUser.getRole());

        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, result));
    }

    @GetMapping("{id}")
    public ResponseEntity<Object> getTransactionByID(@PathVariable(value = "id") String id){
        TransactionResponseDto response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, response));
    }

    @Operation(summary = "Transfer money")
    @PostMapping(value = "/transfer")
    public ResponseEntity<Object> transferMoney(@Valid @RequestBody MoneyTransferRequestDto request){
        try{
            transactionService.transferMoney(request);
        } catch (Exception e){
            return ResponseEntity.badRequest().body(apiResponse.response(e.getMessage(), false, null));
        }
        return ResponseEntity.ok(apiResponse.response("Transfer money successfully", true, null));
    }

    @Operation(summary = "Deposit money")
    @PostMapping(value = "/deposit")
    public ResponseEntity<Object> depositMoney(@Valid @RequestBody MoneyUpdateRequest request){
        try{
            transactionService.depositMoney(request);
        } catch (Exception e){
            return ResponseEntity.badRequest().body(apiResponse.response(e.getMessage(), false, null));
        }
        return ResponseEntity.ok(apiResponse.response("Deposit money successfully", true, null));
    }

    @Operation(summary = "Withdraw money")
    @PostMapping(value = "/withdraw")
    public ResponseEntity<Object> withdrawMoney(@Valid @RequestBody MoneyUpdateRequest request){
        try{
            transactionService.withdrawMoney(request);
        } catch (Exception e){
            return ResponseEntity.badRequest().body(apiResponse.response(e.getMessage(), false, null));
        }
        return ResponseEntity.ok(apiResponse.response("Withdraw money successfully", true, null));
    }

    @Operation(summary = "Get transactions by date")
    @GetMapping(value = "/date")
    public ResponseEntity<Object> getTransactionsByDate(@RequestParam(value = "offset", defaultValue = "0") String offset,
                                                        @RequestParam(value = "limit", defaultValue = "10") String limit){
        PaginDto<TransactionResponseDto> paginDto = new PaginDto<>();
        paginDto.setOffset(offset);
        paginDto.setLimit(limit);

        PaginDto<TransactionResponseDto> result = transactionService.getTransactionsOrderByDate(paginDto);

        return ResponseEntity.ok(apiResponse.response("Retrieved data successfully", true, result));
    }
}
