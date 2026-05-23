package com.phonebiz.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "phone_history")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneHistory extends BaseEntity {

    @Column(name = "phone_id", nullable = false)
    private Long phoneId;

    @Column(name = "phone_number", nullable = false, length = 50)
    private String phoneNumber;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "from_status")
    private Integer fromStatus;

    @Column(name = "to_status")
    private Integer toStatus;

    @Column(name = "from_employee_no", length = 50)
    private String fromEmployeeNo;

    @Column(name = "to_employee_no", length = 50)
    private String toEmployeeNo;

    @Column(name = "from_org", length = 200)
    private String fromOrg;

    @Column(name = "to_org", length = 200)
    private String toOrg;

    @Column(name = "work_order_no", length = 50)
    private String workOrderNo;

    @Column(nullable = false, length = 50)
    private String operator;

    @Column(name = "operated_at", nullable = false)
    private LocalDateTime operatedAt;

    @Column(length = 500)
    private String remark;

    @PrePersist
    protected void onCreate() {
        if (operatedAt == null) operatedAt = LocalDateTime.now();
    }
}
