package com.phonebiz.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_user")
public class SysUser extends BaseEntity {

    public static final int USER_INACTIVE = 0;
    public static final int USER_ACTIVE = 1;


    public static final int USER_ADMIN = 1;
    public static final int USER_OPS = 2;
    public static final int USER_FINANCE = 3;
    public static final int USER_BOSS = 4;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    @Column(name = "employee_no", nullable = false, unique = true, length = 20)
    private String employeeNo;
    @Column(nullable = false)
    private Integer role;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "scope_org_id")
    private Long scopeOrgId;
    @Column(nullable = false)
    private Integer status = SysUser.USER_ACTIVE;

    @Column(name = "login_fail_count", nullable = false)
    private Integer loginFailCount = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    public boolean needsPasswordChange() {
        return passwordChangedAt == null;
    }

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
