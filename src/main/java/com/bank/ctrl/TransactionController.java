package com.bank.ctrl;

import com.bank.dto.PaginDto;
import com.bank.dto.response.TransactionResponseDto;
import com.bank.sv.TransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions")
public class TransactionController {
    @Autowired
    private TransactionService transactionService;

    @GetMapping
    public ResponseEntity<Object> getTransactions(@RequestParam(value = "offset", defaultValue = "0") String offset,
                                              @RequestParam(value = "limit", defaultValue = "10") String limit,
                                              @RequestParam(value = "keyword", required = false) String keyword){
        PaginDto<TransactionResponseDto> pagin = new PaginDto<>();
        pagin.setOffset(offset);
        pagin.setLimit(limit);
        pagin.setKeyword(keyword);

        PaginDto<TransactionResponseDto> result = transactionService.getTransactions(pagin);

        return ResponseEntity.ok(result);
    }

    @GetMapping("{id}")
    public ResponseEntity<Object> getTransactionByID(@PathVariable(value = "id") String id){
        TransactionResponseDto response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(response);
    }
}
