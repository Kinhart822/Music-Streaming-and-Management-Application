package com.spring.dto.request.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminPresentation {
    private Long id;
    private String avatar;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String gender;
    private Instant birthDay;
    private String phone;
    private Integer status;
    private Long createdBy;
    private Long lastModifiedBy;
    private Instant createdDate;
    private Instant lastModifiedDate;
}
