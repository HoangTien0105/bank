package com.bank.exception;

import com.bank.dto.response.ResponseDto;
import com.bank.utils.APIResponse;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private APIResponse apiResponse;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        FieldError firstError = ex.getBindingResult().getFieldErrors().get(0);
        String errorMessage = firstError.getDefaultMessage();

        return ResponseEntity.badRequest().body(apiResponse.response(errorMessage, false, null));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResponseDto> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(apiResponse.response(ex.getMessage(), false, null));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ResponseDto> handleValidationException(ValidationException ex) {
        return ResponseEntity.badRequest().body(apiResponse.response(ex.getMessage(), false, null));
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseDto> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(apiResponse.response(ex.getMessage(), false, null));
    }
}
