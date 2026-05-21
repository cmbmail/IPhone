package com.phonebiz.entity;

import java.time.LocalDate;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "employee")
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_no", nullable = false, unique = true, length = 20)
    private String employeeNo;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(length = 50)
    private String position;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status = EmployeeStatus.active;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Column(name = "leave_date")
    private LocalDate leaveDate;

    @Column(name = "is_virtual", nullable = false)
    private Boolean isVirtual = false;

    public enum EmployeeStatus {
        active, inactive
    }
}

