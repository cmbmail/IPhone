package com.phonebiz.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.UserVO;
import com.phonebiz.entity.Employee;
import com.phonebiz.entity.OrgStructure;
import com.phonebiz.entity.SysRole;
import com.phonebiz.entity.SysUser;
import com.phonebiz.repository.EmployeeRepository;
import com.phonebiz.repository.OrgStructureRepository;
import com.phonebiz.repository.SysRoleRepository;
import com.phonebiz.repository.SysUserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final OrgStructureRepository orgRepository;
    private final SysRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${phonebiz.default-password}")
    private String defaultPassword;

    // ======== Legacy methods ========

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
        user.setPasswordHash(passwordEncoder.encode(defaultPassword));
        user.setEmployeeNo(employee.getEmployeeNo());
        user.setRole(SysUser.USER_OPS);
        user.setScopeOrgId(employee.getOrgId());
        user.setStatus(SysUser.USER_ACTIVE);
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

    // ======== User List (combined Employee + SysUser) ========

    @Transactional(readOnly = true)
    public List<UserVO> getUsersByOrg(Long orgId) {
        List<Long> orgIds = getOrgIdsWithDescendants(orgId);
        List<Employee> employees = employeeRepository.findByOrgIdInAndStatusActive(orgIds);
        return buildUserVOList(employees);
    }

    @Transactional(readOnly = true)
    public List<UserVO> getAllUsers() {
        List<Employee> employees = employeeRepository.findAllActive();
        return buildUserVOList(employees);
    }

    private List<Long> getOrgIdsWithDescendants(Long orgId) {
        List<Long> orgIds = new ArrayList<>();
        orgIds.add(orgId);
        OrgStructure org = orgRepository.findById(orgId).orElse(null);
        if (org != null) {
            String pathPrefix = org.getPath() + "/";
            List<OrgStructure> descendants = orgRepository.findByPathStartingWith(pathPrefix);
            for (OrgStructure d : descendants) {
                orgIds.add(d.getId());
            }
        }
        return orgIds;
    }

    private List<UserVO> buildUserVOList(List<Employee> employees) {
        // Batch load all SysUsers by employee numbers (was N+1)
        List<String> employeeNos = employees.stream().map(Employee::getEmployeeNo)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<String, SysUser> userMap = new HashMap<>();
        if (!employeeNos.isEmpty()) {
            userRepository.findAllByEmployeeNoIn(employeeNos)
                    .forEach(u -> userMap.put(u.getEmployeeNo(), u));
        }

        // Batch load org names (was N+1)
        Set<Long> orgIdSet = employees.stream().map(Employee::getOrgId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> orgNameMap = new HashMap<>();
        if (!orgIdSet.isEmpty()) {
            orgRepository.findAllById(orgIdSet).forEach(o -> orgNameMap.put(o.getId(), o.getName()));
        }

        // Batch load role names (was N+1 per unique role)
        Set<Long> roleIdSet = userMap.values().stream()
                .map(SysUser::getRoleId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> roleNameMap = new HashMap<>();
        if (!roleIdSet.isEmpty()) {
            roleRepository.findAllById(roleIdSet).forEach(r -> roleNameMap.put(r.getId(), r.getName()));
        }

        List<UserVO> result = new ArrayList<>();
        for (Employee emp : employees) {
            SysUser user = userMap.get(emp.getEmployeeNo());
            UserVO vo = new UserVO();
            vo.setEmployeeId(emp.getId());
            vo.setName(emp.getName());

            if (user != null) {
                vo.setId(user.getId());
                vo.setUsername(user.getUsername());
                vo.setRoleId(user.getRoleId());
                vo.setStatus(user.getStatus());
                vo.setUpdatedAt(user.getUpdatedAt());
            } else {
                vo.setUsername(emp.getEmployeeNo());
                vo.setStatus(Employee.EMP_ACTIVE);
                vo.setUpdatedAt(emp.getUpdatedAt());
            }

            vo.setOrgId(emp.getOrgId());
            vo.setOrgName(orgNameMap.getOrDefault(emp.getOrgId(), "-"));
            vo.setRoleName(user != null && user.getRoleId() != null
                    ? roleNameMap.getOrDefault(user.getRoleId(), "-")
                    : mapLegacyRole(user));

            result.add(vo);
        }

        result.sort(Comparator.comparing((UserVO vo) -> vo.getUpdatedAt() != null ? vo.getUpdatedAt() : java.time.LocalDateTime.of(2000,1,1,0,0)).reversed());
        return result;
    }

    private Integer mapRoleIdToEnum(Long roleId) {
        if (roleId == null) return SysUser.USER_OPS;
        return roleRepository.findById(roleId)
                .map(r -> {
                    String code = r.getCode() != null ? r.getCode().toLowerCase() : "";
                    return switch (code) {
                        case "admin" -> SysUser.USER_ADMIN;
                        case "ops" -> SysUser.USER_OPS;
                        case "finance" -> SysUser.USER_FINANCE;
                        case "boss" -> SysUser.USER_BOSS;
                        default -> SysUser.USER_OPS;
                    };
                })
                .orElse(SysUser.USER_OPS);
    }

    private String mapLegacyRole(SysUser user) {
        if (user == null || user.getRole() == null) return "-";
        return switch (user.getRole()) {
            case 1 -> "系统管理员";
            case 2 -> "运维人员";
            case 3 -> "财务人员";
            case 4 -> "管理层";
            default -> "-";
        };
    }

    // ======== User Management ========

    @Transactional
    public void updateUsername(Long employeeId, String newUsername, String operator) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        SysUser user = userRepository.findByEmployeeNo(employee.getEmployeeNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        if (newUsername == null || newUsername.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
        }

        newUsername = newUsername.trim();
        if (!newUsername.equals(user.getUsername()) && userRepository.existsByUsername(newUsername)) {
            throw new BusinessException(ErrorCode.USER_002);
        }

        String oldUsername = user.getUsername();
        user.setUsername(newUsername);
        user.setUpdatedBy(operator);
        userRepository.save(user);

        log.info("User username changed from {} to {} by {}", oldUsername, newUsername, operator);
    }

    @Transactional
    public void updateDepartment(Long employeeId, Long newOrgId, String operator) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        if (newOrgId == null) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
        }

        orgRepository.findById(newOrgId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORG_001));

        employee.setOrgId(newOrgId);
        employee.setUpdatedBy(operator);
        employeeRepository.save(employee);

        // Update sys_user scopeOrgId
        userRepository.findByEmployeeNo(employee.getEmployeeNo()).ifPresent(user -> {
            user.setScopeOrgId(newOrgId);
            user.setUpdatedBy(operator);
            userRepository.save(user);
            log.info("User {} department changed to org {} by {}", user.getUsername(), newOrgId, operator);
        });
    }

    @Transactional
    public void resetPassword(Long employeeId, String operator) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        SysUser user = userRepository.findByEmployeeNo(employee.getEmployeeNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        user.setPasswordHash(passwordEncoder.encode(defaultPassword));
        user.setPasswordChangedAt(null); // force password change on next login
        user.setUpdatedBy(operator);
        userRepository.save(user);

        log.info("Password reset for user {} by {}", user.getUsername(), operator);
    }

    @Transactional
    public void disableUser(Long employeeId, String operator) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        SysUser user = userRepository.findByEmployeeNo(employee.getEmployeeNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        user.setStatus(SysUser.USER_INACTIVE);
        user.setUpdatedBy(operator);
        userRepository.save(user);

        log.info("User {} disabled by {}", user.getUsername(), operator);
    }

    @Transactional
    public void enableUser(Long employeeId, String operator) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        SysUser user = userRepository.findByEmployeeNo(employee.getEmployeeNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        user.setStatus(SysUser.USER_ACTIVE);
        user.setUpdatedBy(operator);
        userRepository.save(user);

        log.info("User {} enabled by {}", user.getUsername(), operator);
    }

    @Transactional
    public void deleteUser(Long employeeId, String operator) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        SysUser user = userRepository.findByEmployeeNo(employee.getEmployeeNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        // Delete sys_user
        user.setDeletedAt(LocalDateTime.now()); userRepository.save(user);
        // Deactivate employee
        employee.setStatus(Employee.EMP_INACTIVE);
        employee.setUpdatedBy(operator);
        employeeRepository.save(employee);

        log.info("User {} deleted by {}", user.getUsername(), operator);
    }

    @Transactional
    public void updateRole(Long employeeId, Long roleId, String operator) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
        SysUser user = userRepository.findByEmployeeNo(employee.getEmployeeNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
        user.setRoleId(roleId);
        // Sync legacy role enum for backward compatibility
        user.setRole(mapRoleIdToEnum(roleId));
        user.setUpdatedBy(operator);
        userRepository.save(user);
        log.info("User {} role updated to {} by {}", user.getUsername(), roleId, operator);
    }
}