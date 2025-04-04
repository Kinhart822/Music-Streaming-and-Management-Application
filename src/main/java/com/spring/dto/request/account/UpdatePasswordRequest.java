package com.spring.dto.request.account;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordRequest {
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String newPassword;

    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String confirmPassword;
}
