package com.spring.exceptions;

import com.spring.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends RuntimeException {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageSource messageSource;

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse> handleBusinessException(BusinessException be, HttpServletRequest request) {
        log.error("BusinessException occurred: {}", be.toString());
        int status = Integer.parseInt(be.getStatus());
        return ResponseEntity
                .status(status)
                .body(new ApiResponse(be.getStatus(), be.getMessage(), be.getDescription()));
    }
    // fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(Exception e) {
        log.error("Unknown exception occurred: {}", e.getMessage(), e);
        return ResponseEntity
                .status(500)
                .body(new ApiResponse("500", "INTERNAL_SERVER_ERROR", e.getMessage()));
    }
}