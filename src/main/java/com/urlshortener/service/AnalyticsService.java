package com.urlshortener.service;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.UrlStatisticsResponse;
import com.urlshortener.entity.Analytics;
import com.urlshortener.entity.UrlMapping;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.AnalyticsRepository;
import com.urlshortener.repository.UrlMappingRepository;
import com.urlshortener.util.UserAgentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final UrlMappingRepository urlMappingRepository;
    private final UserAgentParser userAgentParser;

    @Transactional
    public void trackClick(
            String shortCode,
            String ipAddress,
            String userAgent,
            String referer,
            String country,
            String city
    ) {
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found: " + shortCode));

        UserAgentParser.ParsedUserAgent parsedUA = userAgentParser.parse(userAgent);

        Analytics analytics = Analytics.builder()
                .urlId(urlMapping.getId())
                .shortCode(shortCode)
                .ipAddress(ipAddress != null && ipAddress.length() > 45 ? ipAddress.substring(0, 45) : ipAddress)
                .userAgent(truncateString(userAgent, 512))
                .referer(truncateString(referer, 100))
                .browserName(parsedUA.getBrowserName())
                .browserVersion(parsedUA.getBrowserVersion())
                .operatingSystem(parsedUA.getOperatingSystem())
                .deviceType(parsedUA.getDeviceType())
                .deviceBrand(parsedUA.getDeviceBrand())
                .country(country)
                .city(city)
                .clickedAt(LocalDateTime.now())
                .build();

        analyticsRepository.save(analytics);
        log.debug("Tracked click for: {}", shortCode);
    }

    @Transactional(readOnly = true)
    public Page<AnalyticsResponse> getClickHistory(String shortCode, int page, int size) {
        validateShortCodeExists(shortCode);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Analytics> analyticsPage = analyticsRepository.findByShortCodeOrderByClickedAtDesc(shortCode, pageable);

        return analyticsPage.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<AnalyticsResponse> getClickHistoryByDateRange(
            String shortCode,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size
    ) {
        validateShortCodeExists(shortCode);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Analytics> analyticsPage = analyticsRepository.findByShortCodeAndClickedAtBetween(
                shortCode, startDate, endDate, pageable);

        return analyticsPage.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public UrlStatisticsResponse getUrlStatistics(String shortCode) {
        UrlMapping urlMapping = validateShortCodeExists(shortCode);

        long totalClicks = analyticsRepository.countByShortCode(shortCode);
        long uniqueVisitors = analyticsRepository.countUniqueVisitorsByShortCode(shortCode);

        List<Object[]> browserStats = analyticsRepository.findBrowserStatsByShortCode(shortCode);
        List<Object[]> osStats = analyticsRepository.findOsStatsByShortCode(shortCode);
        List<Object[]> deviceStats = analyticsRepository.findDeviceTypeStatsByShortCode(shortCode);
        List<Object[]> countryStats = analyticsRepository.findCountryStatsByShortCode(shortCode);
        List<Object[]> refererStats = analyticsRepository.findRefererStatsByShortCode(shortCode);
        List<Object[]> hourlyStats = analyticsRepository.findHourlyStatsByShortCode(shortCode);
        List<Object[]> dailyStats = analyticsRepository.findDailyStatsByShortCode(
                shortCode, 
                LocalDateTime.now().minusDays(30), 
                LocalDateTime.now()
        );

        return UrlStatisticsResponse.builder()
                .shortCode(shortCode)
                .originalUrl(urlMapping.getOriginalUrl())
                .totalClicks(totalClicks)
                .uniqueVisitors(uniqueVisitors)
                .firstClick(findFirstClick(shortCode))
                .lastClick(findLastClick(shortCode))
                .browsers(convertToStatEntries(browserStats, totalClicks))
                .operatingSystems(convertToStatEntries(osStats, totalClicks))
                .devices(convertToStatEntries(deviceStats, totalClicks))
                .countries(convertToStatEntries(countryStats, totalClicks))
                .referers(convertToStatEntries(refererStats, totalClicks))
                .hourlyDistribution(convertToHourlyEntries(hourlyStats))
                .dailyDistribution(convertToDailyEntries(dailyStats))
                .build();
    }

    @Transactional(readOnly = true)
    public long getTotalClicks(String shortCode) {
        return analyticsRepository.countByShortCode(shortCode);
    }

    @Transactional(readOnly = true)
    public long getUniqueVisitors(String shortCode) {
        return analyticsRepository.countUniqueVisitorsByShortCode(shortCode);
    }

    @Transactional(readOnly = true)
    public LocalDateTime getLastClickTime(String shortCode) {
        Page<Analytics> page = analyticsRepository.findByShortCodeOrderByClickedAtDesc(
                shortCode, PageRequest.of(0, 1));
        return page.getContent().stream()
                .findFirst()
                .map(Analytics::getClickedAt)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<UrlStatisticsResponse.StatEntry> getTopBrowsers(String shortCode, int limit) {
        List<Object[]> stats = analyticsRepository.findBrowserStatsByShortCode(shortCode);
        long total = analyticsRepository.countByShortCode(shortCode);
        
        return stats.stream()
                .limit(limit)
                .map(row -> UrlStatisticsResponse.StatEntry.builder()
                        .name((String) row[0])
                        .count(toLong(row[1]))
                        .percentage(calculatePercentage(toLong(row[1]), total))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UrlStatisticsResponse.StatEntry> getTopCountries(String shortCode, int limit) {
        List<Object[]> stats = analyticsRepository.findCountryStatsByShortCode(shortCode);
        long total = analyticsRepository.countByShortCode(shortCode);
        
        return stats.stream()
                .limit(limit)
                .map(row -> UrlStatisticsResponse.StatEntry.builder()
                        .name(row[0] != null ? (String) row[0] : "Unknown")
                        .count(toLong(row[1]))
                        .percentage(calculatePercentage(toLong(row[1]), total))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UrlStatisticsResponse.StatEntry> getTopReferers(String shortCode, int limit) {
        List<Object[]> stats = analyticsRepository.findRefererStatsByShortCode(shortCode);
        long total = analyticsRepository.countByShortCode(shortCode);
        
        return stats.stream()
                .limit(limit)
                .map(row -> UrlStatisticsResponse.StatEntry.builder()
                        .name(row[0] != null ? (String) row[0] : "Direct")
                        .count(toLong(row[1]))
                        .percentage(calculatePercentage(toLong(row[1]), total))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UrlStatisticsResponse.DailyEntry> getDailyStats(String shortCode, int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        
        List<Object[]> stats = analyticsRepository.findDailyStatsByShortCode(shortCode, startDate, endDate);
        
        return stats.stream()
                .map(row -> UrlStatisticsResponse.DailyEntry.builder()
                        .date(toLocalDateTime(row[0]))
                        .clicks(toLong(row[1]))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAnalyticsForUrl(String urlId) {
        analyticsRepository.deleteByUrlId(urlId);
        log.info("Deleted analytics for URL: {}", urlId);
    }

    private UrlMapping validateShortCodeExists(String shortCode) {
        return urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found: " + shortCode));
    }

    private AnalyticsResponse mapToResponse(Analytics analytics) {
        return AnalyticsResponse.builder()
                .id(analytics.getId())
                .shortCode(analytics.getShortCode())
                .originalUrl(analytics.getUrlMapping() != null ? analytics.getUrlMapping().getOriginalUrl() : null)
                .clickedAt(analytics.getClickedAt())
                .ipAddress(analytics.getIpAddress())
                .country(analytics.getCountry())
                .city(analytics.getCity())
                .browserName(analytics.getBrowserName())
                .browserVersion(analytics.getBrowserVersion())
                .operatingSystem(analytics.getOperatingSystem())
                .deviceType(analytics.getDeviceType())
                .deviceBrand(analytics.getDeviceBrand())
                .referer(analytics.getReferer())
                .build();
    }

    private List<UrlStatisticsResponse.StatEntry> convertToStatEntries(List<Object[]> rawStats, long total) {
        if (total == 0) return new ArrayList<>();
        
        return rawStats.stream()
                .map(row -> UrlStatisticsResponse.StatEntry.builder()
                        .name(row[0] != null ? row[0].toString() : "Unknown")
                        .count(toLong(row[1]))
                        .percentage(calculatePercentage(toLong(row[1]), total))
                        .build())
                .collect(Collectors.toList());
    }

    private List<UrlStatisticsResponse.HourlyEntry> convertToHourlyEntries(List<Object[]> rawStats) {
        return rawStats.stream()
                .map(row -> UrlStatisticsResponse.HourlyEntry.builder()
                        .hour(toInt(row[0]))
                        .clicks(toLong(row[1]))
                        .build())
                .collect(Collectors.toList());
    }

    private List<UrlStatisticsResponse.DailyEntry> convertToDailyEntries(List<Object[]> rawStats) {
        return rawStats.stream()
                .map(row -> UrlStatisticsResponse.DailyEntry.builder()
                        .date(toLocalDateTime(row[0]))
                        .clicks(toLong(row[1]))
                        .build())
                .collect(Collectors.toList());
    }

    private LocalDateTime findFirstClick(String shortCode) {
        Page<Analytics> page = analyticsRepository.findByShortCodeOrderByClickedAtDesc(
                shortCode, PageRequest.of(0, 1000));
        return page.getContent().stream()
                .reduce((first, second) -> second)
                .map(Analytics::getClickedAt)
                .orElse(null);
    }

    private LocalDateTime findLastClick(String shortCode) {
        Page<Analytics> page = analyticsRepository.findByShortCodeOrderByClickedAtDesc(
                shortCode, PageRequest.of(0, 1));
        return page.getContent().stream()
                .findFirst()
                .map(Analytics::getClickedAt)
                .orElse(null);
    }

    private double calculatePercentage(long count, long total) {
        if (total == 0) return 0.0;
        return Math.round((double) count / total * 10000) / 100.0;
    }

    private String truncateString(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate().atStartOfDay();
        }
        if (value instanceof java.time.LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }
}