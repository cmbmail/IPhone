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
@Table(name = "announcement")
public class Announcement extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;
    public static final int STATUS_ARCHIVED = 2;


    public static final int PRIORITY_URGENT = 1;
    public static final int PRIORITY_HIGH = 2;
    public static final int PRIORITY_NORMAL = 3;
    public static final int PRIORITY_LOW = 4;


    public static final int TYPE_SYSTEM = 1;
    public static final int TYPE_MAINTENANCE = 2;
    public static final int TYPE_POLICY = 3;
    public static final int TYPE_OPERATION = 4;
    public static final int TYPE_OTHER = 5;


    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    @Column(name = "announcement_type", nullable = false, length = 30)
    private Integer announcementType;
    @Column(name = "priority", nullable = false, length = 20)
    private Integer priority;
    @Column(name = "status", nullable = false, length = 20)
    private Integer status;
}
