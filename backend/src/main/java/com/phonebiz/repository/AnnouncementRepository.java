package com.phonebiz.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.phonebiz.entity.Announcement;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Page<Announcement> findByStatus(Announcement.AnnouncementStatus status, Pageable pageable);

    List<Announcement> findTop5ByStatusOrderByCreatedAtDesc(Announcement.AnnouncementStatus status);

    Page<Announcement> findByAnnouncementType(Announcement.AnnouncementType type, Pageable pageable);

    long countByStatus(Announcement.AnnouncementStatus status);
}
