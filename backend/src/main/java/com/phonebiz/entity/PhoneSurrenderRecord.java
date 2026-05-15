package com.phonebiz.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "phone_surrender_record")
public class PhoneSurrenderRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "surrender_type", nullable = false, length = 20)
    private String surrenderType;

    @Column(nullable = false, length = 50)
    private String operator;

    @Column(name = "work_order_no", length = 50)
    private String workOrderNo;

    @Column(length = 500)
    private String remark;

    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt = LocalDateTime.now();
}
