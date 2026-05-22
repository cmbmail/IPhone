package com.phonebiz.entity;

import java.time.LocalDate;
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
@Table(name = "phone_surrender_record")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneSurrenderRecord extends BaseEntity {

    public static final int SURRENDER_TYPE_DECOMMISSION = 1;
    public static final int SURRENDER_TYPE_CANCEL = 2;

    @Column(name = "phone_id", nullable = false)
    private Long phoneId;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "final_user", length = 20)
    private String finalUser;

    @Column(name = "final_org", length = 200)
    private String finalOrg;

    @Column(name = "surrender_date", nullable = false)
    private LocalDate surrenderDate;

    @Column(name = "surrender_type", nullable = false)
    @Builder.Default
    private Integer surrenderType = SURRENDER_TYPE_DECOMMISSION;

    @Column(nullable = false, length = 50)
    private String operator;

    @Column(name = "work_order_no", length = 50)
    private String workOrderNo;

    @Column(length = 500)
    private String remark;

    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;

    @PrePersist
    protected void onCreate() {
        if (archivedAt == null) archivedAt = LocalDateTime.now();
    }
}
