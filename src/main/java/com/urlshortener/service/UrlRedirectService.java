package com.urlshortener.service;

import com.urlshortener.entity.UrlMapping;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlInactiveException;
import com.urlshortener.repository.UrlMappingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service responsible for URL redirection operations.
 * Implements cache-aside pattern with Redis for high performance.
 */
@Service
@Slf4j
public class UrlRedirectService {

    private final UrlMappingRepository urlMappingRepository;
    private final RedisCacheService redisCacheService;
    private final AnalyticsService analyticsService;

    public UrlRedirectService(
            UrlMappingRepository urlMappingRepository,
            RedisCacheService redisCacheService,
            @Lazy AnalyticsService analyticsService
    ) {
        this.urlMappingRepository = urlMappingRepository;
        this.redisCacheService = redisCacheService;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public RedirectInfo resolveAndRedirect(
            String shortCode,
            String ipAddress,
            String userAgent,
            String referer
    ) {
        log.debug("Resolving redirect for short code: {}", shortCode);

        Optional<String> cachedUrl = redisCacheService.getCachedOriginalUrl(shortCode);
        String originalUrl;
        UrlMapping urlMapping;

        if (cachedUrl.isPresent()) {
            log.debug("Cache hit for: {}", shortCode);
            originalUrl = cachedUrl.get();
            urlMapping = urlMappingRepository.findByShortCode(shortCode)
                    .orElseThrow(() -> new ResourceNotFoundException("URL not found: " + shortCode));
        } else {
            log.debug("Cache miss for: {}, fetching from database", shortCode);
            urlMapping = findActiveUrlMapping(shortCode);
            validateNotExpired(urlMapping);
            originalUrl = urlMapping.getOriginalUrl();
            redisCacheService.cacheOriginalUrl(shortCode, originalUrl);
        }

        validateActive(urlMapping);
        validateNotExpired(urlMapping);

        analyticsService.trackClick(shortCode, ipAddress, userAgent, referer, null, null);
        urlMappingRepository.incrementClickCount(shortCode);

        log.info("Redirect resolved: {} -> {}", shortCode, originalUrl);

        return new RedirectInfo(
                originalUrl,
                shortCode,
                urlMapping.getClickCount() + 1,
                true
        );
    }

    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {
        Optional<String> cachedUrl = redisCacheService.getCachedOriginalUrl(shortCode);
        
        if (cachedUrl.isPresent()) {
            log.debug("Cache hit for URL lookup: {}", shortCode);
            return cachedUrl.get();
        }

        log.debug("Cache miss for URL lookup: {}", shortCode);
        UrlMapping urlMapping = findActiveUrlMapping(shortCode);
        validateNotExpired(urlMapping);
        redisCacheService.cacheOriginalUrl(shortCode, urlMapping.getOriginalUrl());

        return urlMapping.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public UrlMapping getUrlMapping(String shortCode) {
        Optional<UrlMapping> cached = redisCacheService.getCachedUrlMapping(shortCode);
        
        if (cached.isPresent()) {
            log.debug("Cache hit for URL mapping: {}", shortCode);
            return cached.get();
        }

        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found: " + shortCode));

        redisCacheService.cacheUrlMapping(shortCode, urlMapping);
        return urlMapping;
    }

    @Transactional(readOnly = true)
    public boolean isRedirectable(String shortCode) {
        Optional<String> cachedUrl = redisCacheService.getCachedOriginalUrl(shortCode);
        if (cachedUrl.isPresent()) {
            return true;
        }
        return urlMappingRepository.findByShortCodeAndActiveTrue(shortCode)
                .map(urlMapping -> !isExpired(urlMapping))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public void warmCache(Iterable<String> shortCodes) {
        log.info("Warming cache for short codes");
        for (String shortCode : shortCodes) {
            try {
                urlMappingRepository.findByShortCode(shortCode)
                        .ifPresent(urlMapping -> {
                            redisCacheService.cacheUrlMapping(shortCode, urlMapping);
                            redisCacheService.cacheOriginalUrl(shortCode, urlMapping.getOriginalUrl());
                        });
            } catch (Exception e) {
                log.warn("Failed to warm cache for: {}", shortCode, e);
            }
        }
    }

    private UrlMapping findActiveUrlMapping(String shortCode) {
        return urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> {
                    log.warn("Short code not found: {}", shortCode);
                    return new ResourceNotFoundException("URL not found for short code: " + shortCode);
                });
    }

    private void validateActive(UrlMapping urlMapping) {
        if (!urlMapping.getActive()) {
            log.warn("Attempted redirect to inactive URL: {}", urlMapping.getShortCode());
            redisCacheService.invalidateCache(urlMapping.getShortCode());
            throw new UrlInactiveException("This URL has been deactivated");
        }
    }

    private void validateNotExpired(UrlMapping urlMapping) {
        if (isExpired(urlMapping)) {
            log.warn("Attempted redirect to expired URL: {}", urlMapping.getShortCode());
            redisCacheService.invalidateCache(urlMapping.getShortCode());
            throw new UrlExpiredException("This URL has expired");
        }
    }

    private boolean isExpired(UrlMapping urlMapping) {
        return urlMapping.getExpiryDate() != null &&
               urlMapping.getExpiryDate().isBefore(LocalDateTime.now());
    }

    public record RedirectInfo(
            String originalUrl,
            String shortCode,
            Long clickCount,
            boolean redirectable
    ) {}
}