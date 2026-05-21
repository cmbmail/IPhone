package com.phonebiz.entity;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "phone_number")
public class PhoneNumber extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "user_id", length = 20)
    private String userId;

    @Column(name = "extension_number", length = 10)
    private String extensionNumber;

    @Column(name = "extension_type", length = 20)
    private String extensionType;

    @Column(name = "is_shared", nullable = false)
    private Boolean isShared = false;

    @Column(name = "is_reentry", nullable = false)
    private Boolean isReentry = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhoneStatus status = PhoneStatus.idle;

    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "allocation_org_id")
    private Long allocationOrgId;

    @Column(length = 500)
    private String remark;

    @Column(nullable = false)
    @Version
    private Long version = 0L;

    public enum PhoneStatus {
        idle, active, stopped, cancelled, reserved, disabled
    }
}
