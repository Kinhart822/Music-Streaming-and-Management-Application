package com.spring.service;

import com.spring.dto.request.account.RefreshTokenRequest;
import com.spring.dto.request.account.SignInRequest;
import com.spring.dto.response.ApiResponse;

import java.util.Map;

public interface AuthService {
    Map<String, String> signIn(SignInRequest signInRequest);

    Map<String, String> refresh(RefreshTokenRequest refreshRequest);

    ApiResponse signOut(String accessToken);
}
