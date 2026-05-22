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
@Table(name = "announcement")
public class Announcement extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "announcement_type", nullable = false, length = 30)
    private AnnouncementType announcementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private AnnouncementPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnnouncementStatus status;

    public enum AnnouncementType {
        SYSTEM, MAINTENANCE, POLICY, OPERATION, OTHER
    }

    public enum AnnouncementPriority {
        URGENT, HIGH, NORMAL, LOW
    }

    public enum AnnouncementStatus {
        DRAFT, PUBLISHED, ARCHIVED
    }
}
