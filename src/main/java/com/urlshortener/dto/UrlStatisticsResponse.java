package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Aggregated click statistics for a short URL")
public class UrlStatisticsResponse {

    @Schema(description = "Short code", example = "abc1234")
    private String shortCode;

    @Schema(description = "Original long URL", example = "https://example.com")
    private String originalUrl;

    @Schema(description = "Total number of clicks", example = "150")
    private long totalClicks;

    @Schema(description = "Number of unique visitors by IP", example = "42")
    private long uniqueVisitors;

    @Schema(description = "Timestamp of the first click")
    private LocalDateTime firstClick;

    @Schema(description = "Timestamp of the most recent click")
    private LocalDateTime lastClick;

    @Schema(description = "Click breakdown by browser")
    private List<StatEntry> browsers;

    @Schema(description = "Click breakdown by operating system")
    private List<StatEntry> operatingSystems;

    @Schema(description = "Click breakdown by device type")
    private List<StatEntry> devices;

    @Schema(description = "Click breakdown by country")
    private List<StatEntry> countries;

    @Schema(description = "Click breakdown by referer")
    private List<StatEntry> referers;

    @Schema(description = "Click distribution by hour of day (24h format)")
    private List<HourlyEntry> hourlyDistribution;

    @Schema(description = "Click distribution by day")
    private List<DailyEntry> dailyDistribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "A single statistics entry with name, count, and percentage")
    public static class StatEntry {
        @Schema(description = "Entry name", example = "Chrome")
        private String name;

        @Schema(description = "Click count", example = "85")
        private long count;

        @Schema(description = "Percentage of total clicks", example = "56.67")
        private double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Hourly click count entry")
    public static class HourlyEntry {
        @Schema(description = "Hour of day (0-23)", example = "14")
        private int hour;

        @Schema(description = "Click count for this hour", example = "25")
        private long clicks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Daily click count entry")
    public static class DailyEntry {
        @Schema(description = "Date")
        private LocalDateTime date;

        @Schema(description = "Click count for this day", example = "50")
        private long clicks;
    }
}
