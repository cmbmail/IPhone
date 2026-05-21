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
public class PhoneStatisticsDTO {

    private long totalCount;
    private long allocatedCount;
    private long idleCount;
    private long stoppedCount;
    private long cancelledCount;
    private long reservedCount;
    private long disabledCount;

    private Map<String, Long> statusDistribution;
    private Map<Long, Long> orgDistribution;
    private List<DailyTrend> dailyTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrend {
        private String date;
        private long allocated;
        private long surrendered;
    }
}

