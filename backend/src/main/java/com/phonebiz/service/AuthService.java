package com.phonebiz.service;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.ChangePasswordRequest;
import com.phonebiz.dto.LoginRequest;
import com.phonebiz.dto.LoginResponse;
import com.phonebiz.entity.SysUser;
import com.phonebiz.repository.SysUserRepository;
import com.phonebiz.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${phonebiz.lock.max-fail-count:5}")
    private int maxFailCount;

    @Value("${phonebiz.lock.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        SysUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_001));

        if (user.isLocked()) {
            throw new BusinessException(ErrorCode.AUTH_003,
                    "Account locked until " + user.getLockedUntil());
        }

        if (user.getStatus() == SysUser.UserStatus.inactive) {
            throw new BusinessException(ErrorCode.AUTH_004, "Account is inactive");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            userRepository.updateLastLoginAt(user.getUsername(), LocalDateTime.now());
            user = userRepository.findByUsername(user.getUsername()).orElseThrow();

        } catch (BadCredentialsException e) {
            int failCount = user.getLoginFailCount() + 1;
            userRepository.updateLoginFailCount(user.getUsername(), failCount);

            if (failCount >= maxFailCount) {
                LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(lockDurationMinutes);
                userRepository.updateLockedUntil(user.getUsername(), lockedUntil);
                throw new BusinessException(ErrorCode.AUTH_003,
                        "Account locked due to " + failCount + " failed attempts. Locked until " + lockedUntil);
            }

            throw new BusinessException(ErrorCode.AUTH_002,
                    "Invalid credentials. " + (maxFailCount - failCount) + " attempts remaining");
        } catch (LockedException e) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getScopeOrgId());
        return LoginResponse.from(user, token, jwtUtil.getExpiration());
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_001));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_002, "Old password incorrect");
        }

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        userRepository.updatePassword(username, newPasswordHash, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public LoginResponse.UserInfo getCurrentUser(String username) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_001));

        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .employeeNo(user.getEmployeeNo())
                .role(user.getRole().name())
                .scopeOrgId(user.getScopeOrgId())
                .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
                .needsPasswordChange(user.needsPasswordChange())
                .build();
    }
}
