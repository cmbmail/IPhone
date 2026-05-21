package com.phonebiz.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sys_feature_flag")
public class SysFeatureFlag extends BaseEntity {

    @Column(name = "feature_key", unique = true, nullable = false, length = 100)
    private String featureKey;

    @Column(name = "feature_name", nullable = false, length = 200)
    private String featureName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    @Column(name = "scope_type", length = 20)
    private String scopeType;

    @Column(name = "scope_value", length = 200)
    private String scopeValue;

    @Column(name = "start_time")
    private java.time.LocalDateTime startTime;

    @Column(name = "end_time")
    private java.time.LocalDateTime endTime;

    public enum ScopeType {
        ALL,
        ORGANIZATION,
        USER,
        CUSTOM
    }
}
