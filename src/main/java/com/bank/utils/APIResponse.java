package com.bank.utils;

import com.bank.dto.response.ResponseDto;

public interface APIResponse {
    @SuppressWarnings("rawtypes")
    public <T> ResponseDto response(String message, boolean success, T response);
}
