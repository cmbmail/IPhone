package com.phonebiz.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_role")
public class SysRole extends BaseEntity {

    public static final int ROLE_INACTIVE = 0;
    public static final int ROLE_ACTIVE = 1;


    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 200)
    private String description;
    @Column(nullable = false, length = 20)
    private Integer status = SysRole.ROLE_ACTIVE;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;
}
