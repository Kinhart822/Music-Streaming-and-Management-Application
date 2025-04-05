package com.spring.constants;

import lombok.Getter;

@Getter
public enum ApiEndpoints {
    PERMITTED(
            "/api/v1/auth/sign-in",
            "/api/v1/auth/refresh",
            "/api/v1/account/user/sign-up/**",
            "/api/v1/account/user/forgot-password/**"
    );

    private final String[] apis;

    ApiEndpoints(String... apis) {
        this.apis = apis;
    }

}
