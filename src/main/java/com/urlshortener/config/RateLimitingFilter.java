package com.urlshortener.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.dto.ApiResponse;
import com.urlshortener.service.RateLimitingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        String path = request.getRequestURI();
        String method = request.getMethod();

        RateLimitingService.EndpointGroup group = resolveEndpointGroup(path, method);

        RateLimitingService.RateLimitResult result = rateLimitingService.checkRateLimit(clientIp, group);

        if (!result.allowed()) {
            writeRateLimitResponse(response, result.retryAfterSeconds());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader(HEADER_X_REAL_IP);
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }

        String remoteAddr = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "127.0.0.1".equals(remoteAddr)) {
            return "127.0.0.1";
        }

        return remoteAddr;
    }

    private RateLimitingService.EndpointGroup resolveEndpointGroup(String path, String method) {
        if (path.startsWith("/api/v1/auth/")) {
            return RateLimitingService.EndpointGroup.AUTH;
        }

        if (path.equals("/api/v1/urls") && "POST".equalsIgnoreCase(method)) {
            return RateLimitingService.EndpointGroup.URL_CREATE;
        }

        if (path.startsWith("/api/v1/urls/batch") && "POST".equalsIgnoreCase(method)) {
            return RateLimitingService.EndpointGroup.URL_CREATE;
        }

        return RateLimitingService.EndpointGroup.GENERAL;
    }

    private void writeRateLimitResponse(HttpServletResponse response,
                                        long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setHeader("X-RateLimit-Reset", String.valueOf(
                System.currentTimeMillis() / 1000 + retryAfterSeconds));

        ApiResponse<Object> body = ApiResponse.error(
                "Too many requests. Please try again in " + retryAfterSeconds + " seconds.");

        objectMapper.writeValue(response.getWriter(), body);
    }
}
