package com.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating a short URL")
public class UrlMappingRequest {

    @NotBlank(message = "Original URL is required")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    @Pattern(
            regexp = "^https?://[\\w.-]+(?:\\.[a-zA-Z]{2,})+(?:/[^\\s]*)?$",
            message = "Invalid URL format. URL must be a valid HTTP/HTTPS URL"
    )
    @Schema(description = "The original long URL to shorten", example = "https://example.com/very-long-url-that-needs-shortening", requiredMode = Schema.RequiredMode.REQUIRED)
    private String originalUrl;

    @Size(min = 3, max = 50, message = "Custom alias must be between 3 and 50 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9][a-zA-Z0-9_-]*$",
            message = "Custom alias must start with alphanumeric and can only contain letters, numbers, hyphens and underscores"
    )
    @Schema(description = "Optional custom alias for the short URL", example = "my-custom-link")
    private String customAlias;

    @Min(value = 1, message = "Expiry days must be at least 1")
    @Max(value = 3650, message = "Expiry days cannot exceed 3650 (10 years)")
    @Schema(description = "Number of days until the URL expires (alternative to expiryDate)", example = "30")
    private Integer expiryDays;

    @Future(message = "Expiry date must be in the future")
    @Schema(description = "Specific expiration date and time (alternative to expiryDays)", example = "2025-01-01T00:00:00")
    private LocalDateTime expiryDate;

    @AssertTrue(message = "Either expiryDays or expiryDate must be provided, not both")
    private boolean isValidExpiryCombination() {
        if (expiryDays != null && expiryDate != null) {
            return false;
        }
        return true;
    }
}
