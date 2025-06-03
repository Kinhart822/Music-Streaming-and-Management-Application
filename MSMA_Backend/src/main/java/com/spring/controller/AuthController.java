package com.spring.controller;

import com.spring.dto.request.account.RefreshTokenRequest;
import com.spring.dto.request.account.SignInRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.service.impl.AuthServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthServiceImpl authService;

    @PostMapping("/sign-in")
    public ResponseEntity<Map<String, String>> signIn(@RequestBody SignInRequest signInRequest) {
        return ResponseEntity.ok(authService.signIn(signInRequest));
    }

    @PutMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestBody RefreshTokenRequest refreshRequest) {
        return ResponseEntity.ok(authService.refresh(refreshRequest));
    }

    @PutMapping("/sign-out")
    public ResponseEntity<ApiResponse> signOut(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(authService.signOut(token.replace("Bearer ", "")));
    }
}
