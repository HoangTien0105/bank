package com.bank.utils;

import com.bank.dto.response.ResponseDto;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Builder
public class APIResponseImpl implements APIResponse {

    @Override
    public <T> ResponseDto<?> response(String message, boolean success, T response) {
        return ResponseDto.<T>builder()
                .response(response)
                .statusCode(success ? HttpStatus.OK.value() : HttpStatus.BAD_REQUEST.value())
                .message(message)
                .success(success)
                .build();
    }
}