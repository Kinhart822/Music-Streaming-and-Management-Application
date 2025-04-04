package com.spring.constants;

import com.spring.exceptions.BusinessException;
import lombok.Getter;

@Getter
public enum UserType {
    ADMIN,
    USER,
    ARTIST
}