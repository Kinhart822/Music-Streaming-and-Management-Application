package com.spring.constants;

import com.spring.exceptions.BusinessException;
import lombok.Getter;

@Getter
public enum UserType {
    ADMIN(0),
    USER(1),
    ARTIST(2),;

    private final int code;
    UserType(int code) {
        this.code = code;
    }
    public static String getName(int code) {
        for (UserType userType : UserType.values()) {
            if (userType.code == code) {
                return userType.name();
            }
        }
        throw new BusinessException(ApiResponseCode.INVALID_TYPE);
    }
}