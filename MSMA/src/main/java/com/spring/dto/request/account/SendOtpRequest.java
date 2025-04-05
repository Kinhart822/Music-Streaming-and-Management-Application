package com.spring.dto.request.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendOtpRequest {
    private String sessionId;

    @NotBlank
    @Email
    private String email;
}
