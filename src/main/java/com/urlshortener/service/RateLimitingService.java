package com.urlshortener.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitingService {

    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<List<Long>> rateLimitScript;

    @Value("${app.rate-limit.default.capacity:100}")
    private int defaultCapacity;

    @Value("${app.rate-limit.default.window-seconds:60}")
    private int defaultWindowSeconds;

    @Value("${app.rate-limit.url-create.capacity:20}")
    private int urlCreateCapacity;

    @Value("${app.rate-limit.url-create.window-seconds:60}")
    private int urlCreateWindowSeconds;

    @Value("${app.rate-limit.auth.capacity:10}")
    private int authCapacity;

    @Value("${app.rate-limit.auth.window-seconds:60}")
    private int authWindowSeconds;

    @PostConstruct
    public void init() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/rate_limit.lua")));
        rateLimitScript.setResultType((Class<List<Long>>) (Class<?>) List.class);
    }

    public enum EndpointGroup {
        GENERAL, URL_CREATE, AUTH
    }

    public record RateLimitResult(boolean allowed, long retryAfterSeconds) {}

    public RateLimitResult checkRateLimit(String ipAddress, EndpointGroup group) {
        String key = KEY_PREFIX + group.name().toLowerCase() + ":" + ipAddress;
        long now = Instant.now().toEpochMilli();
        String member = now + ":" + UUID.randomUUID().toString();

        int capacity;
        int windowSeconds;

        switch (group) {
            case URL_CREATE:
                capacity = urlCreateCapacity;
                windowSeconds = urlCreateWindowSeconds;
                break;
            case AUTH:
                capacity = authCapacity;
                windowSeconds = authWindowSeconds;
                break;
            default:
                capacity = defaultCapacity;
                windowSeconds = defaultWindowSeconds;
        }

        long windowMillis = windowSeconds * 1000L;

        try {
            List<Long> result = stringRedisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(now),
                    String.valueOf(windowMillis),
                    String.valueOf(capacity),
                    member
            );

            if (result != null && result.size() >= 2) {
                boolean allowed = result.get(0) == 1L;
                long retryAfter = result.get(1);
                if (!allowed) {
                    log.warn("Rate limit exceeded for IP: {} on group: {}, retry after: {}s", ipAddress, group, retryAfter);
                }
                return new RateLimitResult(allowed, retryAfter);
            }

            return new RateLimitResult(true, 0);
        } catch (Exception e) {
            log.error("Rate limit check failed for IP: {}, allowing request", ipAddress, e);
            return new RateLimitResult(true, 0);
        }
    }
}
