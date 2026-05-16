package com.urlshortener.controller;

import com.urlshortener.dto.*;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.UrlManagementService;
import com.urlshortener.service.UrlRedirectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "URL Management", description = "Create, manage, and redirect short URLs")
public class UrlController {

    private final UrlManagementService urlManagementService;
    private final UrlRedirectService urlRedirectService;
    private final AnalyticsService analyticsService;

    @Operation(summary = "Create short URL", description = "Creates a new short URL from a long URL. Optionally accepts a custom alias and expiration settings.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Short URL created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"success\":true,\"message\":\"Short URL created successfully\",\"data\":{\"id\":\"1\",\"shortCode\":\"abc1234\",\"shortUrl\":\"http://localhost:8080/abc1234\",\"originalUrl\":\"https://example.com/very-long-url\",\"createdAt\":\"2024-01-01T00:00:00\",\"expiryDate\":\"2024-01-31T00:00:00\",\"clickCount\":0,\"active\":true,\"remainingDays\":30,\"status\":\"active\"}}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid URL format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Custom alias already taken or conflict"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many requests - rate limit exceeded")
    })
    @PostMapping(value = "/api/v1/urls", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UrlMappingResponse>> createShortUrl(
            @Valid @RequestBody UrlMappingRequest request
    ) {
        log.info("Creating short URL for: {}", request.getOriginalUrl());
        UrlMappingResponse response = urlManagementService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Short URL created successfully", response));
    }

    @Operation(summary = "Batch create short URLs", description = "Creates multiple short URLs in a single request. Max 100 URLs per batch.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Batch processing complete",
                    content = @Content(mediaType = "application/json")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many requests - rate limit exceeded")
    })
    @PostMapping(value = "/api/v1/urls/batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<BatchUrlResponse>> createBatchShortUrls(
            @Valid @RequestBody BatchUrlRequest request
    ) {
        log.info("Creating {} short URLs in batch", request.getUrls().size());
        BatchUrlResponse response = urlManagementService.createBatchShortUrls(request);
        String message = String.format("Batch complete: %d successful, %d failed",
                response.getSuccessCount(), response.getFailureCount());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, response));
    }

    @Operation(summary = "Redirect to original URL", description = "Public endpoint. Redirects a short code to its original URL. Tracks the click for analytics.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "301", description = "Redirect to original URL"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "URL has expired or is inactive")
    })
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToOriginalUrl(
            @Parameter(description = "Short code to resolve", example = "abc1234")
            @PathVariable String shortCode,
            HttpServletRequest httpRequest
    ) {
        log.debug("Redirect request for short code: {}", shortCode);

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String referer = httpRequest.getHeader("Referer");

        UrlRedirectService.RedirectInfo redirectInfo = urlRedirectService.resolveAndRedirect(
                shortCode, ipAddress, userAgent, referer
        );

        log.info("Redirected {} -> {}", shortCode, redirectInfo.originalUrl());

        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .header(HttpHeaders.LOCATION, redirectInfo.originalUrl())
                .build();
    }

    @Operation(summary = "Get URL details", description = "Returns details of a specific short URL.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "URL details retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping(value = "/api/v1/urls/{shortCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UrlMappingResponse>> getUrlDetails(
            @Parameter(description = "Short code to look up", example = "abc1234")
            @PathVariable String shortCode
    ) {
        log.info("Fetching details for short code: {}", shortCode);
        UrlMappingResponse response = urlManagementService.getUrlDetails(shortCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Update URL", description = "Updates the original URL, custom alias, or expiry of an existing short URL.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "URL updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping(value = "/api/v1/urls/{shortCode}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UrlMappingResponse>> updateUrl(
            @Parameter(description = "Short code to update", example = "abc1234")
            @PathVariable String shortCode,
            @Valid @RequestBody UrlMappingUpdateRequest request
    ) {
        log.info("Updating short URL: {}", shortCode);
        UrlMappingResponse response = urlManagementService.updateUrl(shortCode, request);
        return ResponseEntity.ok(ApiResponse.success("URL updated successfully", response));
    }

    @Operation(summary = "Deactivate URL", description = "Deactivates a short URL so it can no longer be accessed.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "URL deactivated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping(value = "/api/v1/urls/{shortCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> deleteShortUrl(
            @Parameter(description = "Short code to deactivate", example = "abc1234")
            @PathVariable String shortCode
    ) {
        log.info("Deactivating short URL: {}", shortCode);
        urlManagementService.deactivateUrl(shortCode);
        return ResponseEntity.ok(ApiResponse.success("URL deactivated successfully", null));
    }

    @Operation(summary = "Activate URL", description = "Re-activates a previously deactivated short URL.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "URL activated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(value = "/api/v1/urls/{shortCode}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> activateUrl(
            @Parameter(description = "Short code to activate", example = "abc1234")
            @PathVariable String shortCode
    ) {
        log.info("Activating short URL: {}", shortCode);
        urlManagementService.activateUrl(shortCode);
        return ResponseEntity.ok(ApiResponse.success("URL activated successfully", null));
    }

    @Operation(summary = "Extend expiration", description = "Extends the expiration date of a short URL by the specified number of days.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expiration extended successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Short code not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(value = "/api/v1/urls/{shortCode}/extend", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> extendExpiration(
            @Parameter(description = "Short code", example = "abc1234")
            @PathVariable String shortCode,
            @Parameter(description = "Number of days to extend (default 30)", example = "30")
            @RequestParam(defaultValue = "30") int days
    ) {
        log.info("Extending expiration for {} by {} days", shortCode, days);
        urlManagementService.extendExpiration(shortCode, days);
        return ResponseEntity.ok(ApiResponse.success("Expiration extended successfully", null));
    }

    @Operation(summary = "List active URLs", description = "Returns a paginated list of the authenticated user's active short URLs.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active URLs retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping(value = "/api/v1/urls", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Page<UrlMappingResponse>>> getActiveUrls(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Fetching active URLs page: {}", page);
        Page<UrlMappingResponse> urls = urlManagementService.getActiveUrls(page, size);
        return ResponseEntity.ok(ApiResponse.paginated(urls, (int) urls.getTotalElements(), page, size));
    }

    @Operation(summary = "List inactive URLs", description = "Returns a paginated list of the authenticated user's deactivated short URLs.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inactive URLs retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping(value = "/api/v1/urls/inactive", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Page<UrlMappingResponse>>> getInactiveUrls(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Fetching inactive URLs page: {}", page);
        Page<UrlMappingResponse> urls = urlManagementService.getInactiveUrls(page, size);
        return ResponseEntity.ok(ApiResponse.paginated(urls, (int) urls.getTotalElements(), page, size));
    }

    @Operation(summary = "List expired URLs", description = "Returns a paginated list of the authenticated user's expired short URLs.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expired URLs retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping(value = "/api/v1/urls/expired", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Page<UrlMappingResponse>>> getExpiredUrls(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Fetching expired URLs page: {}", page);
        Page<UrlMappingResponse> urls = urlManagementService.getExpiredUrls(page, size);
        return ResponseEntity.ok(ApiResponse.paginated(urls, (int) urls.getTotalElements(), page, size));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
