package com.urlshortener.controller;

import com.urlshortener.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Check", description = "Application health monitoring endpoints")
public class HealthController {

    @Operation(summary = "Health check", description = "Returns the current health status of the application. Use this endpoint for monitoring and load balancer health probes.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Service is healthy",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = "{\"success\":true,\"message\":\"Service is healthy\",\"data\":{\"status\":\"UP\",\"timestamp\":\"2024-01-01T00:00:00\",\"service\":\"URL Shortener\",\"version\":\"1.0.0\"}}")))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "URL Shortener");
        health.put("version", "1.0.0");
        return ResponseEntity.ok(ApiResponse.success("Service is healthy", health));
    }
}
