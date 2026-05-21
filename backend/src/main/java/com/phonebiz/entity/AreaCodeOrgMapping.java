package com.phonebiz.entity;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "area_code_org_mapping")
public class AreaCodeOrgMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "area_code", nullable = false, length = 10)
    private String areaCode;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(nullable = false)
    private Integer priority = 1;
}
