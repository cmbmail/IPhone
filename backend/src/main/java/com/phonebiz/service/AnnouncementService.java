package com.phonebiz.service;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;
import com.phonebiz.entity.Announcement;
import com.phonebiz.repository.AnnouncementRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    @Transactional(readOnly = true)
    public Page<Announcement> getAnnouncements(Announcement.AnnouncementStatus status,
                                                Announcement.AnnouncementType type,
                                                Pageable pageable) {
        if (status != null) {
            return announcementRepository.findByStatus(status, pageable);
        }
        if (type != null) {
            return announcementRepository.findByAnnouncementType(type, pageable);
        }
        return announcementRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Announcement getAnnouncementById(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_002, "Announcement not found"));
    }

    @Transactional(readOnly = true)
    public List<Announcement> getLatestPublished(int count) {
        return announcementRepository.findTop5ByStatusOrderByCreatedAtDesc(
                Announcement.AnnouncementStatus.PUBLISHED);
    }

    @Transactional
    public Announcement createAnnouncement(Announcement announcement) {
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setUpdatedAt(LocalDateTime.now());
        return announcementRepository.save(announcement);
    }

    @Transactional
    public Announcement updateAnnouncement(Long id, Announcement updated) {
        Announcement existing = getAnnouncementById(id);
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setAnnouncementType(updated.getAnnouncementType());
        existing.setPriority(updated.getPriority());
        existing.setStatus(updated.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        return announcementRepository.save(existing);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        announcementRepository.deleteById(id);
    }

    @Transactional
    public Announcement publishAnnouncement(Long id) {
        Announcement a = getAnnouncementById(id);
        a.setStatus(Announcement.AnnouncementStatus.PUBLISHED);
        a.setUpdatedAt(LocalDateTime.now());
        return announcementRepository.save(a);
    }

    @Transactional
    public Announcement archiveAnnouncement(Long id) {
        Announcement a = getAnnouncementById(id);
        a.setStatus(Announcement.AnnouncementStatus.ARCHIVED);
        a.setUpdatedAt(LocalDateTime.now());
        return announcementRepository.save(a);
    }
}
