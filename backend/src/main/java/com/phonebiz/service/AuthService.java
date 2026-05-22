package com.phonebiz.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.ChangePasswordRequest;
import com.phonebiz.dto.LoginRequest;
import com.phonebiz.dto.LoginResponse;
import com.phonebiz.entity.SysPermission;
import com.phonebiz.entity.SysUser;
import com.phonebiz.repository.SysPermissionRepository;
import com.phonebiz.repository.SysRolePermissionRepository;
import com.phonebiz.repository.SysUserRepository;
import com.phonebiz.security.JwtUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository userRepository;
    private final SysRolePermissionRepository rolePermissionRepository;
    private final SysPermissionRepository permissionRepository;
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
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_004, "Authentication failed"));

        if (user.isLocked()) {
            throw new BusinessException(ErrorCode.AUTH_004, "Authentication failed");
        }

        if (user.getStatus() == SysUser.USER_INACTIVE) {
            throw new BusinessException(ErrorCode.AUTH_004, "Authentication failed");
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
                throw new BusinessException(ErrorCode.AUTH_004, "Authentication failed");
            }

            throw new BusinessException(ErrorCode.AUTH_004, "Authentication failed");
        } catch (LockedException e) {
            throw new BusinessException(ErrorCode.AUTH_004, "Authentication failed");
        }

        // Load permissions from role_id
        List<String> permissionCodes = loadPermissionCodes(user);

        String token = jwtUtil.generateToken(
                user.getUsername(),
                String.valueOf(user.getRole()),
                user.getScopeOrgId(),
                user.getRoleId(),
                permissionCodes
        );

        LoginResponse response = LoginResponse.from(user, token, jwtUtil.getExpiration());
        if (user.needsPasswordChange()) {
            response.setForceChangePassword(true);
        }
        return response;
    }

    /** Load permission codes from role_id via sys_role_permission + sys_permission */
    private List<String> loadPermissionCodes(SysUser user) {
        if (user.getRoleId() == null) {
            log.warn("User {} has no roleId, assigning empty permissions", user.getUsername());
            return Collections.emptyList();
        }
        try {
            List<Long> permIds = rolePermissionRepository.findPermissionIdsByRoleId(user.getRoleId());
            if (permIds.isEmpty()) {
                return Collections.emptyList();
            }
            List<SysPermission> perms = permissionRepository.findByIdIn(permIds);
            return perms.stream()
                    .map(SysPermission::getCode)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to load permissions for user {} roleId={}: {}",
                    user.getUsername(), user.getRoleId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_004, "Authentication failed"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_002, "Old password incorrect");
        }

        // L-03: Prevent password reuse
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_002, "\u65b0\u5bc6\u7801\u4e0d\u80fd\u4e0e\u5f53\u524d\u5bc6\u7801\u76f8\u540c");
        }

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        LocalDateTime now = LocalDateTime.now();
        userRepository.updatePassword(username, newPasswordHash, now);
        // M-02: Update passwordChangedAt for token invalidation (re-read to avoid overwriting password hash)
        SysUser freshUser = userRepository.findByUsername(username).orElseThrow();
        freshUser.setPasswordChangedAt(now);
        userRepository.save(freshUser);
    }

    @Transactional(readOnly = true)
    public void verifyPassword(String username, String rawPassword) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_004, "Authentication failed"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_002, "密码错误");
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse.UserInfo getCurrentUser(String username) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_004, "Authentication failed"));

        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .employeeNo(user.getEmployeeNo())
                .role(String.valueOf(user.getRole()))
                .scopeOrgId(user.getScopeOrgId())
                .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
                .needsPasswordChange(user.needsPasswordChange())
                .build();
    }
}
