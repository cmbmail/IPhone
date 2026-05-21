package com.phonebiz.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.phonebiz.common.ApiResponse;
import com.phonebiz.dto.DeviceStatisticsDTO;
import com.phonebiz.dto.PhoneStatisticsDTO;
import com.phonebiz.entity.Device;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.service.StatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/statistics")
@PreAuthorize("hasAuthority('phone:view') or hasAuthority('device:view') or hasRole('ADMIN')")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/phones")
    public ApiResponse<PhoneStatisticsDTO> getPhoneStatistics() {
        return ApiResponse.success(statisticsService.getPhoneStatistics());
    }

    @GetMapping("/phones/org/{orgId}")
    public ApiResponse<PhoneStatisticsDTO> getPhoneStatisticsByOrg(@PathVariable Long orgId) {
        return ApiResponse.success(statisticsService.getPhoneStatisticsByOrg(orgId));
    }

    @GetMapping("/phones/status/{status}")
    public ApiResponse<Page<PhoneNumber>> getPhonesByStatus(
            @PathVariable String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PhoneNumber.PhoneStatus phoneStatus = com.phonebiz.common.EnumHelper.parse(PhoneNumber.PhoneStatus.class, status);
        return ApiResponse.success(statisticsService.getPhonesByStatus(phoneStatus, pageable));
    }

    @GetMapping("/devices")
    public ApiResponse<DeviceStatisticsDTO> getDeviceStatistics() {
        return ApiResponse.success(statisticsService.getDeviceStatistics());
    }

    @GetMapping("/devices/offline")
    public ApiResponse<List<Device>> getOfflineDevices() {
        return ApiResponse.success(statisticsService.getOfflineDevices());
    }
}

