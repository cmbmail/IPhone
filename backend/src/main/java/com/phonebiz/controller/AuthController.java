package com.phonebiz.controller;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.ChangePasswordRequest;
import com.phonebiz.dto.LoginRequest;
import com.phonebiz.dto.LoginResponse;
import com.phonebiz.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponse.UserInfo> getCurrentUser(Authentication authentication) {
        return ApiResponse.success(authService.getCurrentUser(authentication.getName()));
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        authService.changePassword(authentication.getName(), request);
        return ApiResponse.success("Password changed successfully", null);
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP", "service", "PhoneBiz Auth"));
    }
}
