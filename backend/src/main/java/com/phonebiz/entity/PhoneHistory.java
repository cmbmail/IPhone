package com.phonebiz.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.Data;

@Data
@Entity
@Table(name = "phone_history")
public class PhoneHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_id", nullable = false)
    private Long phoneId;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", length = 20)
    private String toStatus;

    @Column(name = "from_user", length = 20)
    private String fromUser;

    @Column(name = "to_user", length = 20)
    private String toUser;

    @Column(name = "from_org", length = 200)
    private String fromOrg;

    @Column(name = "to_org", length = 200)
    private String toOrg;

    @Column(name = "work_order_no", length = 50)
    private String workOrderNo;

    @Column(nullable = false, length = 50)
    private String operator;

    @Column(name = "operated_at", nullable = false)
    private LocalDateTime operatedAt = LocalDateTime.now();

    @Column(length = 500)
    private String remark;
}

