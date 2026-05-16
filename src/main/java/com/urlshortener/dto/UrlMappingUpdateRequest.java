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
@Schema(description = "Request payload for updating an existing short URL")
public class UrlMappingUpdateRequest {

    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    @Pattern(
            regexp = "^https?://[\\w.-]+(?:\\.[a-zA-Z]{2,})+(?:/[^\\s]*)?$",
            message = "Invalid URL format"
    )
    @Schema(description = "Updated original URL", example = "https://example.com/updated-url")
    private String originalUrl;

    @Size(min = 3, max = 50, message = "Custom alias must be between 3 and 50 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9][a-zA-Z0-9_-]*$",
            message = "Custom alias must start with alphanumeric"
    )
    @Schema(description = "Updated custom alias", example = "updated-alias")
    private String customAlias;

    @Future(message = "Expiry date must be in the future")
    @Schema(description = "Updated expiration date", example = "2025-06-01T00:00:00")
    private LocalDateTime expiryDate;

    @Schema(description = "Whether the URL should be active", example = "true")
    private Boolean active;
}
