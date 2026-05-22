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
        // Use COUNT/GROUP BY instead of findAll() to avoid loading all entities
        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        long totalCount = 0;
        for (Object[] row : phoneRepository.countGroupByStatus()) {
            String status = String.valueOf((Integer) row[0]);
            Long count = (Long) row[1];
            statusDistribution.put(status, count);
            totalCount += count;
        }

        Map<Long, Long> orgDistribution = new LinkedHashMap<>();
        for (Object[] row : phoneRepository.countByOrgIdGroupBy()) {
            Long orgId = (Long) row[0];
            Long count = (Long) row[1];
            orgDistribution.put(orgId, count);
        }

        List<PhoneStatisticsDTO.DailyTrend> dailyTrend = generateDailyTrend(7);

        return PhoneStatisticsDTO.builder()
                .totalCount((int) totalCount)
                .allocatedCount(statusDistribution.getOrDefault("1", 0L))
                .idleCount(statusDistribution.getOrDefault("0", 0L))
                .stoppedCount(statusDistribution.getOrDefault("2", 0L))
                .cancelledCount(statusDistribution.getOrDefault("3", 0L))
                .reservedCount(statusDistribution.getOrDefault("4", 0L))
                .disabledCount(statusDistribution.getOrDefault("5", 0L))
                .statusDistribution(statusDistribution)
                .orgDistribution(orgDistribution)
                .dailyTrend(dailyTrend)
                .build();
    }

    @Transactional(readOnly = true)
    public PhoneStatisticsDTO getPhoneStatisticsByOrg(Long orgId) {
        // Use COUNT/GROUP BY instead of findAll() for org-specific stats
        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        long totalCount = 0;
        for (Object[] row : phoneRepository.countByOrgIdGroupByStatus(orgId)) {
            String status = String.valueOf((Integer) row[0]);
            Long count = (Long) row[1];
            statusDistribution.put(status, count);
            totalCount += count;
        }

        return PhoneStatisticsDTO.builder()
                .totalCount((int) totalCount)
                .allocatedCount(statusDistribution.getOrDefault("1", 0L))
                .idleCount(statusDistribution.getOrDefault("0", 0L))
                .stoppedCount(statusDistribution.getOrDefault("2", 0L))
                .cancelledCount(statusDistribution.getOrDefault("3", 0L))
                .reservedCount(statusDistribution.getOrDefault("4", 0L))
                .disabledCount(statusDistribution.getOrDefault("5", 0L))
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
        // Use COUNT/GROUP BY instead of findAll()
        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        long totalCount = 0;
        long onlineCount = 0;
        long offlineCount = 0;
        long unregisteredCount = 0;
        long disabledCount = 0;
        for (Object[] row : deviceRepository.countGroupByStatus()) {
            String status = String.valueOf((Integer) row[0]);
            Long count = (Long) row[1];
            statusDistribution.put(status, count);
            totalCount += count;
            switch (Integer.parseInt(status)) {
                case 1 -> onlineCount = count;
                case 2 -> offlineCount = count;
                case 3 -> unregisteredCount = count;
                case 4 -> disabledCount = count;
            }
        }

        Map<String, Long> typeDistribution = new LinkedHashMap<>();
        for (Object[] row : deviceRepository.countGroupByType()) {
            String type = String.valueOf((Integer) row[0]);
            Long count = (Long) row[1];
            typeDistribution.put(type, count);
        }

        Map<String, Long> modelDistribution = new LinkedHashMap<>();
        for (Object[] row : deviceRepository.countGroupByModel()) {
            String model = (String) row[0];
            Long count = (Long) row[1];
            modelDistribution.put(model, count);
        }

        double onlineRate = totalCount > 0 ? 
                Math.round(onlineCount * 10000.0 / totalCount) / 100.0 : 0;

        List<DeviceStatisticsDTO.DailyOnlineTrend> dailyTrend = generateDeviceDailyTrend(7, onlineCount, offlineCount);

        return DeviceStatisticsDTO.builder()
                .totalCount((int) totalCount)
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

    private List<DeviceStatisticsDTO.DailyOnlineTrend> generateDeviceDailyTrend(int days, long online, long offline) {
        List<DeviceStatisticsDTO.DailyOnlineTrend> trend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
    public Page<PhoneNumber> getPhonesByStatus(Integer status, Pageable pageable) {
        return phoneRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public List<Device> getOfflineDevices() {
        return deviceRepository.findByStatus(Device.DEV_OFFLINE);
    }
}
