package com.phonebiz.service;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.entity.Employee;
import com.phonebiz.entity.SysUser;
import com.phonebiz.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createUserForEmployee(Employee employee, String operator) {
        if (employee.getEmployeeNo() == null) {
            log.warn("Cannot create sys_user for employee without employee number");
            return;
        }

        if (userRepository.existsByUsername(employee.getEmployeeNo())) {
            log.warn("Sys_user already exists for employee: {}", employee.getEmployeeNo());
            return;
        }

        SysUser user = new SysUser();
        user.setUsername(employee.getEmployeeNo());
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setEmployeeNo(employee.getEmployeeNo());
        user.setRole(SysUser.UserRole.ops);
        user.setStatus(SysUser.UserStatus.active);
        user.setCreatedBy(operator);
        user.setUpdatedBy(operator);

        userRepository.save(user);
        log.info("Created sys_user for employee: {}", employee.getEmployeeNo());
    }

    @Transactional(readOnly = true)
    public SysUser getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_001));
    }

    @Transactional
    public void unlockUser(Long userId, String operator) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_001));

        user.setLockedUntil(null);
        user.setLoginFailCount(0);
        user.setUpdatedBy(operator);
        userRepository.save(user);

        log.info("User {} unlocked by {}", user.getUsername(), operator);
    }
}
