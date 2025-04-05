package com.spring.dto.request.account;

import lombok.Data;

import java.util.List;

@Data
public class CreateAdminFromList {
    private List<CreateAdmin> adminList;
}
