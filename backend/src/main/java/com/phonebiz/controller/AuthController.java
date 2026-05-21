package com.phonebiz.controller;

import java.util.Map;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.ChangePasswordRequest;
import com.phonebiz.dto.LoginRequest;
import com.phonebiz.dto.LoginResponse;
import com.phonebiz.service.AuthService;
import com.phonebiz.annotation.AuditLog;
import com.phonebiz.security.JwtUtil;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    @AuditLog(module = "auth", operation = "用户登录", targetType = "SysUser")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<LoginResponse.UserInfo> getCurrentUser(Authentication authentication) {
        return ApiResponse.success(authService.getCurrentUser(authentication.getName()));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        authService.changePassword(authentication.getName(), request);
        return ApiResponse.success("Password changed successfully", null);
    }

    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<LoginResponse> refreshToken(Authentication authentication, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String newToken = jwtUtil.renewIfNeeded(token);
        if (newToken != null) {
            return ApiResponse.success(LoginResponse.builder()
                    .token(newToken)
                    .expiresIn(jwtUtil.getExpiration())
                    .build());
        }
        return ApiResponse.success(null);
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP", "service", "PhoneBiz Auth"));
    }
}

