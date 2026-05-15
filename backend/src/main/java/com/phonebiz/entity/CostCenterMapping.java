package com.phonebiz.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "cost_center_mapping")
public class CostCenterMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "cost_center_name", nullable = false, length = 100)
    private String costCenterName;

    @Column(name = "cost_center_code", nullable = false, length = 50)
    private String costCenterCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CostCenterStatus status = CostCenterStatus.active;

    public enum CostCenterStatus {
        active, inactive
    }
}
