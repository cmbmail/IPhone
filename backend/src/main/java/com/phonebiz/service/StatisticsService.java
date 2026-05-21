package com.phonebiz.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phonebiz.dto.DeviceStatisticsDTO;
import com.phonebiz.dto.PhoneStatisticsDTO;
import com.phonebiz.entity.Device;
import com.phonebiz.entity.PhoneHistory;
import com.phonebiz.entity.PhoneNumber;
import com.phonebiz.repository.DeviceRepository;
import com.phonebiz.repository.PhoneHistoryRepository;
import com.phonebiz.repository.PhoneNumberRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final PhoneNumberRepository phoneRepository;
    private final PhoneHistoryRepository historyRepository;
    private final DeviceRepository deviceRepository;

    @Transactional(readOnly = true)
    public PhoneStatisticsDTO getPhoneStatistics() {
        List<PhoneNumber> allPhones = phoneRepository.findAll();

        Map<String, Long> statusDistribution = allPhones.stream()
                .collect(Collectors.groupingBy(p -> p.getStatus().name(), Collectors.counting()));

        Map<Long, Long> orgDistribution = allPhones.stream()
                .filter(p -> p.getOrgId() != null)
                .collect(Collectors.groupingBy(PhoneNumber::getOrgId, Collectors.counting()));

        List<PhoneStatisticsDTO.DailyTrend> dailyTrend = generateDailyTrend(7);

        return PhoneStatisticsDTO.builder()
                .totalCount(allPhones.size())
                .allocatedCount(statusDistribution.getOrDefault("active", 0L))
                .idleCount(statusDistribution.getOrDefault("idle", 0L))
                .stoppedCount(statusDistribution.getOrDefault("stopped", 0L))
                .cancelledCount(statusDistribution.getOrDefault("cancelled", 0L))
                .reservedCount(statusDistribution.getOrDefault("reserved", 0L))
                .disabledCount(statusDistribution.getOrDefault("disabled", 0L))
                .statusDistribution(statusDistribution)
                .orgDistribution(orgDistribution)
                .dailyTrend(dailyTrend)
                .build();
    }

    @Transactional(readOnly = true)
    public PhoneStatisticsDTO getPhoneStatisticsByOrg(Long orgId) {
        List<PhoneNumber> orgPhones = phoneRepository.findByOrgId(orgId);

        Map<String, Long> statusDistribution = orgPhones.stream()
                .collect(Collectors.groupingBy(p -> p.getStatus().name(), Collectors.counting()));

        return PhoneStatisticsDTO.builder()
                .totalCount(orgPhones.size())
                .allocatedCount(statusDistribution.getOrDefault("active", 0L))
                .idleCount(statusDistribution.getOrDefault("idle", 0L))
                .stoppedCount(statusDistribution.getOrDefault("stopped", 0L))
                .cancelledCount(statusDistribution.getOrDefault("cancelled", 0L))
                .reservedCount(statusDistribution.getOrDefault("reserved", 0L))
                .disabledCount(statusDistribution.getOrDefault("disabled", 0L))
                .statusDistribution(statusDistribution)
                .build();
    }

    private List<PhoneStatisticsDTO.DailyTrend> generateDailyTrend(int days) {
        List<PhoneStatisticsDTO.DailyTrend> trend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            long allocated = historyRepository.countByActionAndOperatedAtBetween("allocate", startOfDay, endOfDay);
            long surrendered = historyRepository.countByActionAndOperatedAtBetween("surrender", startOfDay, endOfDay);

            trend.add(PhoneStatisticsDTO.DailyTrend.builder()
                    .date(date.format(formatter))
                    .allocated(allocated)
                    .surrendered(surrendered)
                    .build());
        }

        return trend;
    }

    @Transactional(readOnly = true)
    public DeviceStatisticsDTO getDeviceStatistics() {
        List<Device> allDevices = deviceRepository.findAll();

        Map<String, Long> typeDistribution = allDevices.stream()
                .collect(Collectors.groupingBy(d -> d.getDeviceType().name(), Collectors.counting()));

        Map<String, Long> modelDistribution = allDevices.stream()
                .filter(d -> d.getModel() != null && !d.getModel().isEmpty())
                .collect(Collectors.groupingBy(Device::getModel, Collectors.counting()));

        long onlineCount = allDevices.stream()
                .filter(d -> d.getStatus() == Device.DeviceStatus.ONLINE)
                .count();
        long offlineCount = allDevices.stream()
                .filter(d -> d.getStatus() == Device.DeviceStatus.OFFLINE)
                .count();
        long unregisteredCount = allDevices.stream()
                .filter(d -> d.getStatus() == Device.DeviceStatus.UNREGISTERED)
                .count();
        long disabledCount = allDevices.stream()
                .filter(d -> d.getStatus() == Device.DeviceStatus.DISABLED)
                .count();

        double onlineRate = allDevices.isEmpty() ? 0 : 
                Math.round(onlineCount * 10000.0 / allDevices.size()) / 100.0;

        List<DeviceStatisticsDTO.DailyOnlineTrend> dailyTrend = generateDeviceDailyTrend(7);

        return DeviceStatisticsDTO.builder()
                .totalCount(allDevices.size())
                .onlineCount(onlineCount)
                .offlineCount(offlineCount)
                .unregisteredCount(unregisteredCount)
                .disabledCount(disabledCount)
                .onlineRate(onlineRate)
                .typeDistribution(typeDistribution)
                .modelDistribution(modelDistribution)
                .dailyOnlineTrend(dailyTrend)
                .build();
    }

    private List<DeviceStatisticsDTO.DailyOnlineTrend> generateDeviceDailyTrend(int days) {
        List<DeviceStatisticsDTO.DailyOnlineTrend> trend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // Use current device counts as baseline (no historical data yet)
        List<Device> allDevices = deviceRepository.findAll();
        long online = allDevices.stream().filter(d -> d.getStatus() == Device.DeviceStatus.ONLINE).count();
        long offline = allDevices.stream().filter(d -> d.getStatus() == Device.DeviceStatus.OFFLINE).count();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            trend.add(DeviceStatisticsDTO.DailyOnlineTrend.builder()
                    .date(date.format(formatter))
                    .online(online)
                    .offline(offline)
                    .build());
        }

        return trend;
    }

    @Transactional(readOnly = true)
    public Page<PhoneNumber> getPhonesByStatus(PhoneNumber.PhoneStatus status, Pageable pageable) {
        return phoneRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public List<Device> getOfflineDevices() {
        return deviceRepository.findByStatus(Device.DeviceStatus.OFFLINE);
    }
}

