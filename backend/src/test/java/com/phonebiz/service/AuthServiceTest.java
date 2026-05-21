package com.phonebiz.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.phonebiz.common.BusinessException;
import com.phonebiz.dto.LoginResponse;
import com.phonebiz.dto.ChangePasswordRequest;
import com.phonebiz.dto.LoginRequest;
import com.phonebiz.entity.SysUser;
import com.phonebiz.repository.SysUserRepository;
import com.phonebiz.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SysUserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private AuthService authService;

    private SysUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new SysUser();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setPasswordHash("encodedPassword");
        testUser.setRole(SysUser.UserRole.admin);
        testUser.setStatus(SysUser.UserStatus.active);
        testUser.setLoginFailCount(0);
        testUser.setLastLoginAt(null);
        testUser.setPasswordChangedAt(LocalDateTime.now());
        testUser.setScopeOrgId(1L);
    }

    @Test
    @DisplayName("测试登录 - 成功")
    void testLogin_Success() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        doNothing().when(userRepository).updateLastLoginAt(anyString(), any(LocalDateTime.class));
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong())).thenReturn("test-token");
        when(jwtUtil.getExpiration()).thenReturn(3600L);

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password");

        assertDoesNotThrow(() -> authService.login(request));
        verify(userRepository).updateLastLoginAt(anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("测试登录 - 用户不存在")
    void testLogin_UserNotFound() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertTrue(exception.getMessage().contains("Username not found"));
    }

    @Test
    @DisplayName("测试登录 - 密码错误")
    void testLogin_WrongPassword() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        doNothing().when(userRepository).updateLoginFailCount(anyString(), anyInt());
        doNothing().when(userRepository).updateLockedUntil(anyString(), any(LocalDateTime.class));

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertTrue(exception.getMessage().startsWith("Account locked") || exception.getMessage().startsWith("Invalid credentials"));
        verify(userRepository).updateLoginFailCount(anyString(), anyInt());
    }

    @Test
    @DisplayName("测试登录 - 账户不活跃")
    void testLogin_InactiveAccount() {
        testUser.setStatus(SysUser.UserStatus.inactive);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertTrue(exception.getMessage().contains("Account is inactive"));
    }

    @Test
    @DisplayName("测试首次登录需改密码")
    void testLogin_FirstLogin() {
        testUser.setPasswordChangedAt(null);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any())).thenReturn(null);
        doNothing().when(userRepository).updateLastLoginAt(any(), any());
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password");

        LoginResponse response = authService.login(request);
        assertTrue(response.isForceChangePassword());
    }

    @Test
    @DisplayName("测试更改密码 - 成功")
    void testChangePassword_Success() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("old", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncoded");
        doNothing().when(userRepository).updatePassword(anyString(), anyString(), any(LocalDateTime.class));

        ChangePasswordRequest changeRequest = new ChangePasswordRequest();
        changeRequest.setOldPassword("old");
        changeRequest.setNewPassword("newPassword");

        assertDoesNotThrow(() -> authService.changePassword("admin", changeRequest));
        verify(userRepository).updatePassword(anyString(), anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("测试更改密码 - 旧密码错误")
    void testChangePassword_WrongOldPassword() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "encodedPassword")).thenReturn(false);

        ChangePasswordRequest changeRequest = new ChangePasswordRequest();
        changeRequest.setOldPassword("wrong");
        changeRequest.setNewPassword("newPassword");

        BusinessException exception = assertThrows(BusinessException.class, () ->
            authService.changePassword("admin", changeRequest));
        assertTrue(exception.getMessage().contains("Old password incorrect"));
    }

    @Test
    @DisplayName("测试登录 - 账户锁定")
    void testLogin_LockedAccount() {
        testUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertTrue(exception.getMessage().contains("Account locked"));
    }

    @Test
    @DisplayName("测试登录 - 密码错误多次后锁定")
    void testLogin_LockedAfterMultipleFailures() {
        testUser.setLoginFailCount(4);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        doNothing().when(userRepository).updateLoginFailCount(anyString(), anyInt());
        doNothing().when(userRepository).updateLockedUntil(anyString(), any(LocalDateTime.class));

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertTrue(exception.getMessage().contains("Account locked"));
        verify(userRepository).updateLockedUntil(anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("测试获取当前用户 - 成功")
    void testGetCurrentUser_Success() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

        var result = authService.getCurrentUser("admin");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        assertEquals("admin", result.getRole());
    }

    @Test
    @DisplayName("测试获取当前用户 - 用户不存在")
    void testGetCurrentUser_NotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authService.getCurrentUser("unknown"));
    }

    @Test
    @DisplayName("测试获取当前用户 - 需要改密码")
    void testGetCurrentUser_NeedsPasswordChange() {
        testUser.setPasswordChangedAt(null);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

        var result = authService.getCurrentUser("admin");

        assertTrue(result.getNeedsPasswordChange());
    }
}

