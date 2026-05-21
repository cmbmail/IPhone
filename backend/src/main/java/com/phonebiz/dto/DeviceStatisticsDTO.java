package com.phonebiz.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatisticsDTO {

    private long totalCount;
    private long onlineCount;
    private long offlineCount;
    private long unregisteredCount;
    private long disabledCount;
    private double onlineRate;

    private Map<String, Long> typeDistribution;
    private Map<String, Long> modelDistribution;
    private List<DeviceStatisticsDTO.DailyOnlineTrend> dailyOnlineTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyOnlineTrend {
        private String date;
        private long online;
        private long offline;
    }
}

