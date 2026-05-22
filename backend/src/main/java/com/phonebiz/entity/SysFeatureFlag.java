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

    @Column(name = "scope_type")
    private Integer scopeType;

    public static final int SCOPE_ALL = 1;
    public static final int SCOPE_ORGANIZATION = 2;
    public static final int SCOPE_USER = 3;
    public static final int SCOPE_CUSTOM = 4;

    @Column(name = "scope_value", length = 200)
    private String scopeValue;

    @Column(name = "start_time")
    private java.time.LocalDateTime startTime;

    @Column(name = "end_time")
    private java.time.LocalDateTime endTime;

    
}
