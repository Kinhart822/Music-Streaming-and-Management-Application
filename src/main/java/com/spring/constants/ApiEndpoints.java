package com.spring.constants;

import lombok.Getter;

@Getter
public enum ApiEndpoints {
    PERMITTED(
            "/api/v1/auth/sign-in",
            "/api/v1/auth/refresh",
            "/api/v1/account/user/sign-up/**",
            "/api/v1/account/user/forgot-password/**",
            "/api/v1/account/admin/create",
            "/api/v1/account/user/forgot-password/**",
            "/api/v1/account/profile/**",
            "/api/v1/account/song/**",
            "/api/v1/account/search/**",
            "/api/v1/account/signUpArtist",
            "/api/v1/account/user/sign-up/check-email-existence",
            "/api/v1/account/signUpArtist",
            "/api/v1/account/updateArtist"
    );

    private final String[] apis;

    ApiEndpoints(String... apis) {
        this.apis = apis;
    }

}
