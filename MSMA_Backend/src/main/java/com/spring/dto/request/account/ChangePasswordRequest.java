package com.spring.dto.request.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String newPassword;

    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String confirmPassword;
}
