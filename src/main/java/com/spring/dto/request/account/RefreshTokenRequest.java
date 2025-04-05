package com.spring.dto.request.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;

    @NotBlank
    @Email
    private String email;
}
