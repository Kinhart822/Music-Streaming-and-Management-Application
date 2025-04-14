package com.spring.exceptions;

import com.spring.constants.ApiResponseCode;
import com.spring.dto.response.ApiResponse;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final String status;
    private final String message;
    private final String description;

    public BusinessException(ApiResponseCode apiResponseCode) {
        this.status = apiResponseCode.getStatus();
        this.message = apiResponseCode.name();
        this.description = apiResponseCode.getDescription();
    }
}
