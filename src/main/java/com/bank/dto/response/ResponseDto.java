package com.bank.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ResponseDto<T> {
    private String message;
    private Integer statusCode;
    boolean success;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T response;
}
