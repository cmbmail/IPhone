package com.phonebiz.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.dto.CreateRoleRequest;
import com.phonebiz.dto.UpdateRoleRequest;
import com.phonebiz.entity.SysPermission;
import com.phonebiz.entity.SysRole;
import com.phonebiz.repository.SysPermissionRepository;
import com.phonebiz.repository.SysRolePermissionRepository;
import com.phonebiz.repository.SysRoleRepository;
import com.phonebiz.repository.SysUserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleRepository roleRepository;
    private final SysPermissionRepository permissionRepository;
    private final SysRolePermissionRepository rolePermissionRepository;
    private final SysUserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SysRole> getAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SysRole> getActiveRoles() {
        return roleRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public SysRole getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_001));
    }

    @Transactional(readOnly = true)
    public List<SysPermission> getPermissionsByRoleId(Long roleId) {
        List<Long> permissionIds = rolePermissionRepository.findPermissionIdsByRoleId(roleId);
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionRepository.findByIdIn(permissionIds);
    }

    @Transactional(readOnly = true)
    public List<SysPermission> getAllPermissions() {
        return permissionRepository.findAllByOrderByModuleAscSortOrderAsc();
    }

    @Transactional(readOnly = true)
    public Map<String, List<SysPermission>> getPermissionsGroupedByModule() {
        return permissionRepository.findAllByOrderByModuleAscSortOrderAsc().stream()
                .collect(Collectors.groupingBy(SysPermission::getModule,
                        java.util.LinkedHashMap::new, Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public long getUserCountByRoleId(Long roleId) {
        return userRepository.countByRoleId(roleId);
    }

    @Transactional
    public SysRole createRole(CreateRoleRequest request, String operator) {
        if (roleRepository.existsByCode(request.getCode())) {
            throw new BusinessException(ErrorCode.ROLE_002);
        }
        if (roleRepository.existsByName(request.getName())) {
            throw new BusinessException(ErrorCode.ROLE_003);
        }

        SysRole role = new SysRole();
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        role.setStatus(SysRole.ROLE_ACTIVE);
        role.setIsSystem(false);
        role.setCreatedBy(operator);
        role.setUpdatedBy(operator);

        SysRole saved = roleRepository.save(role);

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            rolePermissionRepository.savePermissions(saved.getId(), request.getPermissionIds());
        }

        log.info("Role created: {} ({}) by {}", role.getName(), role.getCode(), operator);
        return saved;
    }

    @Transactional
    public SysRole updateRole(Long id, UpdateRoleRequest request, String operator) {
        SysRole role = getRoleById(id);

        if (request.getName() != null) {
            if (!role.getName().equals(request.getName()) && roleRepository.existsByName(request.getName())) {
                throw new BusinessException(ErrorCode.ROLE_003);
            }
            role.setName(request.getName());
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        if (request.getStatus() != null) {
            role.setStatus(Integer.valueOf(request.getStatus()));
        }

        role.setUpdatedBy(operator);
        SysRole saved = roleRepository.save(role);

        if (request.getPermissionIds() != null) {
            rolePermissionRepository.savePermissions(saved.getId(), request.getPermissionIds());
        }

        log.info("Role updated: {} by {}", role.getName(), operator);
        return saved;
    }

    @Transactional
    public void deleteRole(Long id) {
        SysRole role = getRoleById(id);

        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new BusinessException(ErrorCode.ROLE_004);
        }

        long userCount = userRepository.countByRoleId(id);
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.ROLE_005);
        }

        rolePermissionRepository.deleteByRoleId(id);
        role.setDeletedAt(LocalDateTime.now()); roleRepository.save(role);
        log.info("Role deleted: {} ({})", role.getName(), role.getCode());
    }
}
