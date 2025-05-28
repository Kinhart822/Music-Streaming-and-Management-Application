package com.spring.service.impl;

import com.spring.constants.*;
import com.spring.dto.request.account.*;
import com.spring.dto.response.ApiResponse;
import com.spring.entities.*;
import com.spring.exceptions.*;
import com.spring.repository.RefreshTokenRepository;
import com.spring.repository.NotificationTokenRepository;
import com.spring.security.JwtUtil;
import com.spring.service.AuthService;
import com.spring.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Transactional
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final String ACCESS_TOKEN_KEY = "accessToken";
    private static final String REFRESH_TOKEN_KEY = "refreshToken";
    private static final String USER_TYPE = "userType";
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NotificationTokenRepository notificationTokenRepository;

    //TODO: Sign-in AND refresh
    @Override
    public Map<String, String> signIn(SignInRequest signInRequest) {
        String username = signInRequest.getEmail();
        UserDetails userDetails = userService.loadUserByUsername(username);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, signInRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BusinessException(ApiResponseCode.BAD_CREDENTIALS);
        } catch (Exception e) {
            throw new BusinessException(ApiResponseCode.BAD_REQUEST);
        }

        User user = (User) userDetails;
        Map<String, String> response = new HashMap<>();
        response.put("email", user.getEmail());
        response.put(USER_TYPE, user.getUserType().toString());
        response.put(ACCESS_TOKEN_KEY, jwtUtil.generateAccessToken(userDetails));

        // Save Device Token
        if (!notificationTokenRepository.existsByUserId(user.getId())) {
            if (user.getUserType() == UserType.USER || user.getUserType() == UserType.ARTIST) {
                NotificationToken tokenDevice = NotificationToken.builder()
                        .deviceToken(signInRequest.getDeviceToken())
                        .user(user)
                        .build();
                notificationTokenRepository.save(tokenDevice);
            }
        }

        // Nếu user đã có refresh token, không cấp lại
        if (refreshTokenRepository.existsByUserAndStatus(user, CommonStatus.ACTIVE.getStatus())) {
            response.put(REFRESH_TOKEN_KEY, null);
            return response;
        }

        // Tạo refresh token mới
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);
        refreshTokenRepository.save(RefreshToken.builder()
                .refreshToken(refreshToken)
                .status(CommonStatus.ACTIVE.getStatus())
                .user(user)
                .createdDate(jwtUtil.extractIssuedDate(refreshToken))
                .expirationDate(jwtUtil.extractExpirationDate(refreshToken))
                .build());
        response.put(REFRESH_TOKEN_KEY, refreshToken);

        return response;
    }


    @Transactional(noRollbackFor = {BusinessException.class, ExpiredJwtException.class})
    @Override
    public Map<String, String> refresh(RefreshTokenRequest refreshRequest) {
        String refreshToken = refreshRequest.getRefreshToken();
        UserDetails userDetails = userService.loadUserByUsername(refreshRequest.getEmail());
        try {
            if (!Objects.equals(jwtUtil.extractUsername(refreshToken), refreshRequest.getEmail())) {
                throw new BusinessException(ApiResponseCode.INVALID_REFRESH_REQUEST_USERNAME);
            }
        } catch (ExpiredJwtException e) {
            refreshTokenRepository
                    .saveAll(refreshTokenRepository
                            .findAllByUserAndStatus((User) userDetails, CommonStatus.ACTIVE.getStatus())
                            .stream()
                            .map(r -> r.toBuilder()
                                    .status(CommonStatus.DELETED.getStatus())
                                    .build())
                            .toList());
            throw new BusinessException(ApiResponseCode.INVALID_REFRESH_REQUEST_EXPIRED);
        }
        Map<String, String> response = new HashMap<>();
        response.put(ACCESS_TOKEN_KEY, jwtUtil.generateAccessToken(userDetails));
        return response;
    }

    @Override
    public ApiResponse signOut(String accessToken) {
        refreshTokenRepository
                .saveAll(refreshTokenRepository
                        .findAllByUserAndStatus(
                                (User) userService
                                        .loadUserByUsername(jwtUtil
                                                .extractUsername(accessToken)),
                                CommonStatus.ACTIVE.getStatus())
                        .stream()
                        .map(r -> r.toBuilder()
                                .status(CommonStatus.DELETED.getStatus())
                                .build())
                        .toList());
        return ApiResponse.ok();
    }
}
