package com.bank.ctrl;

import com.bank.constant.Message;
import com.bank.dto.request.UpdateAccountStatusRequestDto;
import com.bank.dto.response.ResponseDto;
import com.bank.sv.AccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PutMapping("{id}")
    public ResponseEntity<Object> updateAccountStatus(@PathVariable("id") String id, @Valid @RequestBody UpdateAccountStatusRequestDto request){
        try{
            accountService.updateAccountStatus(id, request);
        } catch (Exception e){
            return ResponseEntity.badRequest().body(ResponseDto.builder()
                    .errorCode(HttpStatus.BAD_REQUEST.value())
                    .errorDescription(e.getMessage())
                    .success(false)
                    .message(Message.UPDATE_FAIL)
                    .build());
        }
        return ResponseEntity.ok(ResponseDto.builder().success(true).message("Update account successfully").build());
    }
}
