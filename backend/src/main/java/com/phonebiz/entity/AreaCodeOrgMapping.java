package com.phonebiz.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "area_code_org_mapping")
public class AreaCodeOrgMapping extends BaseEntity {


    @Column(name = "area_code", nullable = false, length = 10)
    private String areaCode;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(nullable = false)
    private Integer priority = 1;
}
