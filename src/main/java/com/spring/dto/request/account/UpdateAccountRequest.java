package com.spring.dto.request.account;

import com.spring.constants.Gender;
import com.spring.dto.validator.GenderSubset;
import com.spring.dto.validator.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@Data
public class UpdateAccountRequest {
    private MultipartFile avatar;
    private MultipartFile backgroundImage;
    private String description;
    private String firstName;
    private String lastName;

    @GenderSubset(anyOf = {Gender.Male, Gender.Female, Gender.Other})
    private Gender gender;

    private String dateOfBirth;
    private String phone;
}
