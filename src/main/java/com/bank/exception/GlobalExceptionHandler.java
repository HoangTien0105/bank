package com.bank.exception;

import com.bank.dto.response.ResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex){
        FieldError firstError = ex.getBindingResult().getFieldErrors().get(0);
        String errorMessage = firstError.getDefaultMessage();

        ResponseDto response = ResponseDto.builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(errorMessage)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
