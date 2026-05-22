package com.phonebiz.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "phone_number")
public class PhoneNumber extends BaseEntity {

    public static final int PS_IDLE = 0;
    public static final int PS_ACTIVE = 1;
    public static final int PS_STOPPED = 2;
    public static final int PS_CANCELLED = 3;
    public static final int PS_RESERVED = 4;
    public static final int PS_DISABLED = 5;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 50)
    private String phoneNumber;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "extension_number", length = 20)
    private String extensionNumber;

    @Column(name = "extension_type", length = 50)
    private String extensionType;

    @Column(name = "is_shared", nullable = false)
    private Boolean isShared = false;

    @Column(name = "is_reentry", nullable = false)
    private Boolean isReentry = false;
    @Column(nullable = false)
    private Integer status = PhoneNumber.PS_IDLE;

    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "allocation_org_id")
    private Long allocationOrgId;

    @Column(length = 500)
    private String remark;

    @Column(nullable = false)
    @Version
    private Long version = 0L;
}
