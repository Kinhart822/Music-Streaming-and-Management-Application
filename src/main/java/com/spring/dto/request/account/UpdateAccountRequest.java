package com.spring.dto.request.account;

import com.spring.constants.Gender;
import com.spring.dto.validator.GenderSubset;
import com.spring.dto.validator.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
public class UpdateAccountRequest {
    @NotBlank(message = "avatar must be not blank")
    private String avatar;

    @NotBlank(message = "username must be not blank")
    private String username;

    @NotBlank(message = "firstName must be not blank")
    private String firstName;

    @NotNull(message = "lastName must be not null")
    private String lastName;

    @GenderSubset(anyOf = {Gender.Male, Gender.Female, Gender.Other})
    private Gender gender;

    @NotNull(message = "dateOfBirth must be not null")
    private Instant dateOfBirth;

    @PhoneNumber(message = "phone invalid format")
    private String phone;
}
