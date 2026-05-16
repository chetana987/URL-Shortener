package com.urlshortener.controller;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.ApiResponse;
import com.urlshortener.dto.UrlStatisticsResponse;
import com.urlshortener.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "Click statistics, visitor insights, and usage reports for short URLs")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Get URL statistics", description = "Returns aggregated statistics for a short URL including total clicks, unique visitors, browser breakdown, country breakdown, and time-based distribution.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved",
                    content = @Content(mediaType = "application/json")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/urls/{shortCode}")
    public ResponseEntity<ApiResponse<UrlStatisticsResponse>> getUrlStatistics(
            @Parameter(description = "Short code to analyze", example = "abc1234")
            @PathVariable String shortCode
    ) {
        log.info("Fetching statistics for: {}", shortCode);
        UrlStatisticsResponse stats = analyticsService.getUrlStatistics(shortCode);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(summary = "Get click history", description = "Returns a paginated list of all clicks for a short URL.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Click history retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/urls/{shortCode}/clicks")
    public ResponseEntity<ApiResponse<Page<AnalyticsResponse>>> getClickHistory(
            @Parameter(description = "Short code", example = "abc1234")
            @PathVariable String shortCode,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Fetching click history for: {}", shortCode);
        Page<AnalyticsResponse> clicks = analyticsService.getClickHistory(shortCode, page, size);
        return ResponseEntity.ok(ApiResponse.paginated(
                clicks, 
                (int) clicks.getTotalElements(), 
                page, 
                size
        ));
    }

    @Operation(summary = "Get click history by date range", description = "Returns a paginated list of clicks within a specific date range.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Click history retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/urls/{shortCode}/clicks/range")
    public ResponseEntity<ApiResponse<Page<AnalyticsResponse>>> getClickHistoryByDateRange(
            @Parameter(description = "Short code", example = "abc1234")
            @PathVariable String shortCode,
            @Parameter(description = "Start date (ISO format)", example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", example = "2024-12-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Fetching click history by date range for: {}", shortCode);
        Page<AnalyticsResponse> clicks = analyticsService.getClickHistoryByDateRange(
                shortCode, startDate, endDate, page, size
        );
        return ResponseEntity.ok(ApiResponse.paginated(
                clicks,
                (int) clicks.getTotalElements(),
                page,
                size
        ));
    }

    @Operation(summary = "Get total clicks", description = "Returns the total number of clicks for a short URL.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Total clicks count retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/urls/{shortCode}/total-clicks")
    public ResponseEntity<ApiResponse<Long>> getTotalClicks(
            @Parameter(description = "Short code", example = "abc1234")
            @PathVariable String shortCode
    ) {
        long totalClicks = analyticsService.getTotalClicks(shortCode);
        return ResponseEntity.ok(ApiResponse.success(totalClicks));
    }

    @Operation(summary = "Get unique visitors", description = "Returns the number of unique visitors (by IP) for a short URL.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Unique visitors count retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/urls/{shortCode}/unique-visitors")
    public ResponseEntity<ApiResponse<Long>> getUniqueVisitors(
            @Parameter(description = "Short code", example = "abc1234")
            @PathVariable String shortCode
    ) {
        long uniqueVisitors = analyticsService.getUniqueVisitors(shortCode);
        return ResponseEntity.ok(ApiResponse.success(uniqueVisitors));
    }

    @Operation(summary = "Get last click time", description = "Returns the timestamp of the most recent click on a short URL.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Last click time retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/urls/{shortCode}/last-click")
    public ResponseEntity<ApiResponse<LocalDateTime>> getLastClickTime(
            @Parameter(description = "Short code", example = "abc1234")
            @PathVariable String shortCode
    ) {
        LocalDateTime lastClick = analyticsService.getLastClickTime(shortCode);
        return ResponseEntity.ok(ApiResponse.success(lastClick));
    }

    @Operation(summary = "Get top browsers", description = "Returns the most common browsers used to access the short URL.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Browser stats retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/urls/{shortCode}/browsers")
    public ResponseEntity<ApiResponse<List<UrlStatisticsResponse.StatEntry>>> getTopBrowsers(
            @Parameter(description = "Short code", example = "abc1234")
            @PathVariable String shortCode,
            @Parameter(description = "Max results", example = "10")
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<UrlStatisticsResponse.StatEntry> browsers = analyticsService.getTopBrowsers(shortCode, limit);
        return ResponseEntity.ok(ApiResponse.success(browsers));
    }

    @Operation(summary = "Get top countries", description = "Returns the most common countries where clicks originated.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Country stats retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/urls/{shortCode}/countries")
    public ResponseEntity<ApiResponse<List<UrlStatisticsResponse.StatEntry>>> getTopCountries(
            @Parameter(description = "Short code", example = "abc1234")
            @PathVariable String shortCode,
            @Parameter(description = "Max results", example = "10")
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<UrlStatisticsResponse.StatEntry> countries = analyticsService.getTopCountries(shortCode, limit);
        return ResponseEntity.ok(ApiResponse.success(countries));
    }

    @Operation(summary = "Get top referers", description = "Returns the most common referer URLs for clicks on a short URL.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Referer stats retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/urls/{shortCode}/referers")
    public ResponseEntity<ApiResponse<List<UrlStatisticsResponse.StatEntry>>> getTopReferers(
            @Parameter(description = "Short code", example = "abc1234")
            @PathVariable String shortCode,
            @Parameter(description = "Max results", example = "10")
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<UrlStatisticsResponse.StatEntry> referers = analyticsService.getTopReferers(shortCode, limit);
        return ResponseEntity.ok(ApiResponse.success(referers));
    }

    @Operation(summary = "Get daily stats", description = "Returns click statistics grouped by day for the specified number of days.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Daily stats retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/urls/{shortCode}/daily")
    public ResponseEntity<ApiResponse<List<UrlStatisticsResponse.DailyEntry>>> getDailyStats(
            @Parameter(description = "Short code", example = "abc1234")
            @PathVariable String shortCode,
            @Parameter(description = "Number of days to look back", example = "30")
            @RequestParam(defaultValue = "30") int days
    ) {
        List<UrlStatisticsResponse.DailyEntry> dailyStats = analyticsService.getDailyStats(shortCode, days);
        return ResponseEntity.ok(ApiResponse.success(dailyStats));
    }

    @Operation(summary = "Get hourly stats", description = "Returns click statistics showing how many clicks occurred in each hour of the day (24-hour distribution).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Hourly stats retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/urls/{shortCode}/hourly")
    public ResponseEntity<ApiResponse<List<UrlStatisticsResponse.HourlyEntry>>> getHourlyStats(
            @Parameter(description = "Short code", example = "abc1234")
            @PathVariable String shortCode
    ) {
        UrlStatisticsResponse stats = analyticsService.getUrlStatistics(shortCode);
        return ResponseEntity.ok(ApiResponse.success(stats.getHourlyDistribution()));
    }
}
