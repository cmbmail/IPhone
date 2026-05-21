package com.phonebiz.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.phonebiz.common.BusinessException;
import com.phonebiz.entity.Employee;
import com.phonebiz.entity.SysUser;
import com.phonebiz.repository.SysUserRepository;

@ExtendWith(MockitoExtension.class)
class SysUserServiceTest {

    @Mock
    private SysUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SysUserService sysUserService;

    private Employee testEmployee;
    private SysUser testUser;

    @BeforeEach
    void setUp() {
        testEmployee = new Employee();
        testEmployee.setId(1L);
        testEmployee.setEmployeeNo("EMP001");
        testEmployee.setName("Test Employee");

        testUser = new SysUser();
        testUser.setId(1L);
        testUser.setUsername("EMP001");
        testUser.setPasswordHash("hashed_password");
        testUser.setEmployeeNo("EMP001");
        testUser.setRole(SysUser.UserRole.ops);
        testUser.setStatus(SysUser.UserStatus.active);
    }

    @Test
    @DisplayName("测试为员工创建用户 - 成功")
    void testCreateUserForEmployee_Success() {
        when(userRepository.existsByUsername("EMP001")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any(SysUser.class))).thenReturn(testUser);

        assertDoesNotThrow(() -> sysUserService.createUserForEmployee(testEmployee, "admin"));
        verify(userRepository).save(any(SysUser.class));
    }

    @Test
    @DisplayName("测试为员工创建用户 - 员工号为空")
    void testCreateUserForEmployee_NullEmployeeNo() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setName("Test Employee");

        assertDoesNotThrow(() -> sysUserService.createUserForEmployee(employee, "admin"));
        verify(userRepository, never()).save(any(SysUser.class));
    }

    @Test
    @DisplayName("测试为员工创建用户 - 用户已存在")
    void testCreateUserForEmployee_AlreadyExists() {
        when(userRepository.existsByUsername("EMP001")).thenReturn(true);

        assertDoesNotThrow(() -> sysUserService.createUserForEmployee(testEmployee, "admin"));
        verify(userRepository, never()).save(any(SysUser.class));
    }

    @Test
    @DisplayName("测试按用户名获取用户 - 成功")
    void testGetUserByUsername_Success() {
        when(userRepository.findByUsername("EMP001")).thenReturn(Optional.of(testUser));

        SysUser result = sysUserService.getUserByUsername("EMP001");

        assertNotNull(result);
        assertEquals("EMP001", result.getUsername());
    }

    @Test
    @DisplayName("测试按用户名获取用户 - 不存在")
    void testGetUserByUsername_NotFound() {
        when(userRepository.findByUsername("UNKNOWN")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
            sysUserService.getUserByUsername("UNKNOWN"));
        assertNotNull(exception);
    }

    @Test
    @DisplayName("测试解锁用户 - 成功")
    void testUnlockUser_Success() {
        testUser.setLockedUntil(java.time.LocalDateTime.now().plusHours(1));
        testUser.setLoginFailCount(5);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(SysUser.class))).thenReturn(testUser);

        assertDoesNotThrow(() -> sysUserService.unlockUser(1L, "admin"));
        verify(userRepository).save(any(SysUser.class));
    }

    @Test
    @DisplayName("测试解锁用户 - 用户不存在")
    void testUnlockUser_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
            sysUserService.unlockUser(99L, "admin"));
        assertNotNull(exception);
    }

    @Test
    @DisplayName("测试为员工创建用户 - 验证用户属性")
    void testCreateUserForEmployee_VerifyUserProperties() {
        when(userRepository.existsByUsername("EMP001")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(SysUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sysUserService.createUserForEmployee(testEmployee, "admin");

        verify(userRepository).save(argThat(user ->
            user.getUsername().equals("EMP001") &&
            user.getEmployeeNo().equals("EMP001") &&
            user.getRole() == SysUser.UserRole.ops &&
            user.getStatus() == SysUser.UserStatus.active
        ));
    }
}

