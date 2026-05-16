package com.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload for batch URL creation")
public class BatchUrlResponse {

    @Schema(description = "Number of successfully created URLs", example = "3")
    private int successCount;

    @Schema(description = "Number of failed URL creations", example = "1")
    private int failureCount;

    @Schema(description = "List of successfully created URL mappings")
    private List<UrlMappingResponse> successfulUrls;

    @Schema(description = "List of failed URL creation attempts")
    private List<FailedUrlResponse> failedUrls;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Details of a failed URL creation attempt")
    public static class FailedUrlResponse {
        @Schema(description = "The original URL that failed", example = "https://example.com/invalid")
        private String originalUrl;

        @Schema(description = "Error message describing the failure", example = "Invalid URL format")
        private String error;
    }
}
