package com.spring.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.spring.constants.ApiResponseCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
public class ApiResponse {
    private String status;
    private String message;
    private String description;

    public ApiResponse(ApiResponseCode apiResponseCode) {
        this.status = apiResponseCode.getStatus();
        this.message = apiResponseCode.name();
        this.description = apiResponseCode.getDescription();
    }

    public static ApiResponse ok() {
        return new ApiResponse(ApiResponseCode.SUCCESS);
    }
    public static ApiResponse badRequest() {
        return new ApiResponse(ApiResponseCode.BAD_REQUEST);
    }
}
