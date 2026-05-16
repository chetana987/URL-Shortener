package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Analytics data for a single click event")
public class AnalyticsResponse {

    @Schema(description = "Click event ID", example = "1")
    private Long id;

    @Schema(description = "Short code that was clicked", example = "abc1234")
    private String shortCode;

    @Schema(description = "Original URL that was redirected to", example = "https://example.com")
    private String originalUrl;

    @Schema(description = "Timestamp of the click")
    private LocalDateTime clickedAt;

    @Schema(description = "Visitor IP address", example = "192.168.1.1")
    private String ipAddress;

    @Schema(description = "Visitor country (geo-IP)", example = "United States")
    private String country;

    @Schema(description = "Visitor city (geo-IP)", example = "New York")
    private String city;

    @Schema(description = "Browser name", example = "Chrome")
    private String browserName;

    @Schema(description = "Browser version", example = "120.0.0")
    private String browserVersion;

    @Schema(description = "Operating system", example = "Windows 10")
    private String operatingSystem;

    @Schema(description = "Device type", example = "Desktop")
    private String deviceType;

    @Schema(description = "Device brand", example = "Apple")
    private String deviceBrand;

    @Schema(description = "Referring URL", example = "https://twitter.com")
    private String referer;
}
