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
@Schema(description = "Response payload containing short URL details")
public class UrlMappingResponse {

    @Schema(description = "Internal ID", example = "1")
    private String id;

    @Schema(description = "Short code", example = "abc1234")
    private String shortCode;

    @Schema(description = "Full short URL", example = "http://localhost:8080/abc1234")
    private String shortUrl;

    @Schema(description = "Original long URL", example = "https://example.com/very-long-url")
    private String originalUrl;

    @Schema(description = "Custom alias if set", example = "my-custom-link")
    private String customAlias;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "Expiration date")
    private LocalDateTime expiryDate;

    @Schema(description = "Total click count", example = "42")
    private Long clickCount;

    @Schema(description = "Whether the URL is active", example = "true")
    private Boolean active;

    @Schema(description = "Whether the URL has expired", example = "false")
    private Boolean isExpired;

    @Schema(description = "Days remaining until expiration", example = "30")
    private Long remainingDays;

    @Schema(description = "Status label", example = "active")
    private String status;

    @Schema(description = "Status description", example = "URL is active and accessible")
    private String statusMessage;
}
