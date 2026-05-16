package com.urlshortener.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.entity.UrlMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service for manual Redis cache operations.
 * Implements cache-aside pattern for URL lookups.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private static final String URL_CACHE_PREFIX = "url:";
    private static final String REDIRECT_CACHE_PREFIX = "redirect:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    @Value("${app.cache.url-ttl:3600}")
    private long urlCacheTtl;

    @Value("${app.cache.redirect-ttl:1800}")
    private long redirectCacheTtl;

    /**
     * Caches a URL mapping.
     *
     * @param shortCode The short code (cache key)
     * @param urlMapping The URL mapping to cache
     */
    public void cacheUrlMapping(String shortCode, UrlMapping urlMapping) {
        try {
            String cacheKey = URL_CACHE_PREFIX + shortCode;
            redisTemplate.opsForValue().set(cacheKey, urlMapping, Duration.ofSeconds(urlCacheTtl));
            log.debug("Cached URL mapping: {}", shortCode);
        } catch (Exception e) {
            log.error("Failed to cache URL mapping: {}", shortCode, e);
        }
    }

    /**
     * Retrieves a cached URL mapping.
     *
     * @param shortCode The short code (cache key)
     * @return Optional containing the cached URL mapping
     */
    public Optional<UrlMapping> getCachedUrlMapping(String shortCode) {
        try {
            String cacheKey = URL_CACHE_PREFIX + shortCode;
            Object cached = redisTemplate.opsForValue().get(cacheKey);

            if (cached == null) {
                log.debug("Cache miss for: {}", shortCode);
                return Optional.empty();
            }

            if (cached instanceof UrlMapping) {
                log.debug("Cache hit for: {}", shortCode);
                return Optional.of((UrlMapping) cached);
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get cached URL mapping: {}", shortCode, e);
            return Optional.empty();
        }
    }

    /**
     * Caches the original URL for fast redirects.
     *
     * @param shortCode The short code
     * @param originalUrl The original URL to cache
     */
    public void cacheOriginalUrl(String shortCode, String originalUrl) {
        try {
            String cacheKey = REDIRECT_CACHE_PREFIX + shortCode;
            redisTemplate.opsForValue().set(cacheKey, originalUrl, Duration.ofSeconds(redirectCacheTtl));
            log.debug("Cached redirect: {} -> {}", shortCode, originalUrl);
        } catch (Exception e) {
            log.error("Failed to cache redirect: {}", shortCode, e);
        }
    }

    /**
     * Retrieves a cached original URL.
     *
     * @param shortCode The short code
     * @return Optional containing the original URL
     */
    public Optional<String> getCachedOriginalUrl(String shortCode) {
        try {
            String cacheKey = REDIRECT_CACHE_PREFIX + shortCode;
            Object cached = redisTemplate.opsForValue().get(cacheKey);

            if (cached != null) {
                log.debug("Redirect cache hit for: {}", shortCode);
                return Optional.of(cached.toString());
            }

            log.debug("Redirect cache miss for: {}", shortCode);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get cached redirect: {}", shortCode, e);
            return Optional.empty();
        }
    }

    /**
     * Invalidates all cache entries for a short code.
     * Called when URL is deleted or updated.
     *
     * @param shortCode The short code to invalidate
     */
    public void invalidateCache(String shortCode) {
        try {
            String urlCacheKey = URL_CACHE_PREFIX + shortCode;
            String redirectCacheKey = REDIRECT_CACHE_PREFIX + shortCode;

            redisTemplate.delete(urlCacheKey);
            redisTemplate.delete(redirectCacheKey);

            log.info("Invalidated cache for: {}", shortCode);
        } catch (Exception e) {
            log.error("Failed to invalidate cache: {}", shortCode, e);
        }
    }

    /**
     * Invalidates all URL-related caches.
     */
    public void invalidateAllCaches() {
        try {
            var urlKeys = redisTemplate.keys(URL_CACHE_PREFIX + "*");
            var redirectKeys = redisTemplate.keys(REDIRECT_CACHE_PREFIX + "*");

            if (urlKeys != null && !urlKeys.isEmpty()) {
                redisTemplate.delete(urlKeys);
            }
            if (redirectKeys != null && !redirectKeys.isEmpty()) {
                redisTemplate.delete(redirectKeys);
            }

            log.info("Invalidated all caches");
        } catch (Exception e) {
            log.error("Failed to invalidate all caches", e);
        }
    }

    /**
     * Refreshes cache entry for a short code.
     *
     * @param shortCode The short code
     * @param urlMapping The updated URL mapping
     */
    public void refreshCache(String shortCode, UrlMapping urlMapping) {
        invalidateCache(shortCode);
        cacheUrlMapping(shortCode, urlMapping);
        cacheOriginalUrl(shortCode, urlMapping.getOriginalUrl());
    }

    /**
     * Gets cache statistics.
     *
     * @return Number of cached URL entries
     */
    public long getCacheSize() {
        try {
            var keys = redisTemplate.keys(URL_CACHE_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("Failed to get cache size", e);
            return 0;
        }
    }
}