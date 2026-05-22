package com.phonebiz.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.entity.Announcement;
import com.phonebiz.service.AnnouncementService;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/announcements")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public ApiResponse<Page<Announcement>> getAnnouncements(
            @RequestParam(required = false) Announcement.AnnouncementStatus status,
            @RequestParam(required = false) Announcement.AnnouncementType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(announcementService.getAnnouncements(status, type, pageable));
    }

    @GetMapping("/latest")
    public ApiResponse<List<Announcement>> getLatest() {
        return ApiResponse.success(announcementService.getLatestPublished(5));
    }

    @GetMapping("/{id}")
    public ApiResponse<Announcement> getAnnouncement(@PathVariable Long id) {
        return ApiResponse.success(announcementService.getAnnouncementById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Announcement> createAnnouncement(@RequestBody Announcement announcement) {
        return ApiResponse.success(announcementService.createAnnouncement(announcement));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Announcement> updateAnnouncement(@PathVariable Long id,
                                                         @RequestBody Announcement announcement) {
        return ApiResponse.success(announcementService.updateAnnouncement(id, announcement));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ApiResponse.success("Deleted", null);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Announcement> publishAnnouncement(@PathVariable Long id) {
        return ApiResponse.success(announcementService.publishAnnouncement(id));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Announcement> archiveAnnouncement(@PathVariable Long id) {
        return ApiResponse.success(announcementService.archiveAnnouncement(id));
    }
}
