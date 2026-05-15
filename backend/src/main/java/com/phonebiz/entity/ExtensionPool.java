package com.phonebiz.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "extension_pool")
public class ExtensionPool extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "start_number", nullable = false, length = 10)
    private String startNumber;

    @Column(name = "end_number", nullable = false, length = 10)
    private String endNumber;

    @Column(name = "allocated_by", nullable = false, length = 50)
    private String allocatedBy;
}
