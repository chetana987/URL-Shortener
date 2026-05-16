package com.urlshortener.service;

import com.urlshortener.dto.*;
import com.urlshortener.entity.ClickLog;
import com.urlshortener.entity.UrlMapping;
import com.urlshortener.entity.User;
import com.urlshortener.exception.DuplicateShortCodeException;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.ClickLogRepository;
import com.urlshortener.repository.UrlMappingRepository;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.util.ShortCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlMappingService {

    private final UrlMappingRepository urlMappingRepository;
    private final ClickLogRepository clickLogRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final RedisCacheService redisCacheService;
    private final UserRepository userRepository;

    @Value("${app.url.base-url}")
    private String baseUrl;

    @Transactional
    public UrlMappingResponse createShortUrl(UrlMappingRequest request) {
        String shortCode;

        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            shortCode = request.getCustomAlias().toLowerCase().trim();
            if (urlMappingRepository.existsByShortCode(shortCode) ||
                urlMappingRepository.existsByCustomAlias(shortCode)) {
                throw new DuplicateShortCodeException("Custom alias '" + shortCode + "' already exists");
            }
        } else {
            shortCode = shortCodeGenerator.generate();
            while (urlMappingRepository.existsByShortCode(shortCode)) {
                shortCode = shortCodeGenerator.generate();
            }
        }

        LocalDateTime expiryDate = calculateExpiryDate(request);

        User currentUser = getCurrentUser();

        UrlMapping urlMapping = UrlMapping.builder()
                .shortCode(shortCode)
                .originalUrl(request.getOriginalUrl())
                .customAlias(request.getCustomAlias())
                .expiryDate(expiryDate)
                .user(currentUser)
                .build();

        UrlMapping saved = urlMappingRepository.save(urlMapping);
        
        // Pre-cache the new URL mapping
        redisCacheService.cacheUrlMapping(shortCode, saved);
        redisCacheService.cacheOriginalUrl(shortCode, saved.getOriginalUrl());
        
        log.info("Created URL mapping: {} -> {}", shortCode, request.getOriginalUrl());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {
        // Check cache first using RedisCacheService
        var cachedUrl = redisCacheService.getCachedOriginalUrl(shortCode);
        
        if (cachedUrl.isPresent()) {
            log.debug("Cache hit for: {}", shortCode);
            return cachedUrl.get();
        }

        // Cache miss - fetch from database
        UrlMapping urlMapping = urlMappingRepository.findByShortCodeAndActiveTrue(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL mapping", "shortCode", shortCode));

        if (isExpired(urlMapping)) {
            throw new com.urlshortener.exception.UrlExpiredException("This URL has expired");
        }

        // Cache the result
        redisCacheService.cacheOriginalUrl(shortCode, urlMapping.getOriginalUrl());

        return urlMapping.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public Page<UrlMappingResponse> getAllActiveUrls(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UrlMapping> urls = urlMappingRepository.findAllActiveNonExpired(pageable, LocalDateTime.now());
        return urls.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public UrlMappingResponse getUrlDetails(String shortCode) {
        User currentUser = getCurrentUser();
        // Check cache first
        var cached = redisCacheService.getCachedUrlMapping(shortCode);
        
        if (cached.isPresent()) {
            log.debug("Cache hit for details: {}", shortCode);
            return mapToResponse(cached.get());
        }

        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL mapping", "shortCode", shortCode));
        verifyOwnership(urlMapping, currentUser);
        
        // Cache for future requests
        redisCacheService.cacheUrlMapping(shortCode, urlMapping);
        
        return mapToResponse(urlMapping);
    }

    @Transactional(readOnly = true)
    public UrlMappingDetailResponse getUrlDetailsWithStats(String shortCode) {
        User currentUser = getCurrentUser();
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL mapping", "shortCode", shortCode));
        verifyOwnership(urlMapping, currentUser);

        UrlMappingDetailResponse.ClickStatistics stats = UrlMappingDetailResponse.ClickStatistics.builder()
                .totalClicks(urlMapping.getClickCount())
                .lastClickedAt(clickLogRepository.findByUrlIdOrderByClickedAtDesc(
                        urlMapping.getId(), PageRequest.of(0, 1))
                        .getContent().stream().findFirst().map(ClickLog::getClickedAt).orElse(null))
                .build();

        return UrlMappingDetailResponse.builder()
                .id(urlMapping.getId())
                .shortCode(urlMapping.getShortCode())
                .shortUrl(baseUrl + "/api/v1/urls/redirect/" + urlMapping.getShortCode())
                .originalUrl(urlMapping.getOriginalUrl())
                .customAlias(urlMapping.getCustomAlias())
                .createdAt(urlMapping.getCreatedAt())
                .updatedAt(urlMapping.getUpdatedAt())
                .expiryDate(urlMapping.getExpiryDate())
                .clickCount(urlMapping.getClickCount())
                .active(urlMapping.getActive())
                .isExpired(isExpired(urlMapping))
                .remainingDays(calculateRemainingDays(urlMapping.getExpiryDate()))
                .statistics(stats)
                .build();
    }

    @Transactional
    public UrlMappingResponse updateUrl(String shortCode, UrlMappingUpdateRequest request) {
        User currentUser = getCurrentUser();
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL mapping", "shortCode", shortCode));
        verifyOwnership(urlMapping, currentUser);

        if (request.getOriginalUrl() != null) {
            urlMapping.setOriginalUrl(request.getOriginalUrl());
        }
        if (request.getCustomAlias() != null) {
            urlMapping.setCustomAlias(request.getCustomAlias());
        }
        if (request.getExpiryDate() != null) {
            urlMapping.setExpiryDate(request.getExpiryDate());
        }

        UrlMapping updated = urlMappingRepository.save(urlMapping);
        
        // Invalidate old cache and set new cache
        redisCacheService.refreshCache(shortCode, updated);
        
        log.info("Updated URL mapping: {}", shortCode);

        return mapToResponse(updated);
    }

    @Transactional
    public void deactivateUrl(String shortCode) {
        User currentUser = getCurrentUser();
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL mapping", "shortCode", shortCode));
        verifyOwnership(urlMapping, currentUser);

        urlMapping.setActive(false);
        urlMappingRepository.save(urlMapping);
        
        // Invalidate cache on deletion
        redisCacheService.invalidateCache(shortCode);
        
        log.info("Deactivated URL mapping: {}", shortCode);
    }

    @Transactional
    public void deleteUrl(String shortCode) {
        User currentUser = getCurrentUser();
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL mapping", "shortCode", shortCode));
        verifyOwnership(urlMapping, currentUser);

        // Invalidate cache before deletion
        redisCacheService.invalidateCache(shortCode);
        
        urlMappingRepository.delete(urlMapping);
        
        log.info("Deleted URL mapping: {}", shortCode);
    }

    private LocalDateTime calculateExpiryDate(UrlMappingRequest request) {
        if (request.getExpiryDate() != null) {
            return request.getExpiryDate();
        }
        if (request.getExpiryDays() != null && request.getExpiryDays() > 0) {
            return LocalDateTime.now().plusDays(request.getExpiryDays());
        }
        return null;
    }

    private boolean isExpired(UrlMapping urlMapping) {
        return urlMapping.getExpiryDate() != null &&
               urlMapping.getExpiryDate().isBefore(LocalDateTime.now());
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResourceNotFoundException("User not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void verifyOwnership(UrlMapping urlMapping, User user) {
        if (urlMapping.getUser() == null || !urlMapping.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("URL not found: " + urlMapping.getShortCode());
        }
    }

    private Long calculateRemainingDays(LocalDateTime expiryDate) {
        if (expiryDate == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(LocalDateTime.now(), expiryDate);
    }

    private UrlMappingResponse mapToResponse(UrlMapping urlMapping) {
        return UrlMappingResponse.builder()
                .id(urlMapping.getId())
                .shortCode(urlMapping.getShortCode())
                .shortUrl(baseUrl + "/" + urlMapping.getShortCode())
                .originalUrl(urlMapping.getOriginalUrl())
                .customAlias(urlMapping.getCustomAlias())
                .createdAt(urlMapping.getCreatedAt())
                .updatedAt(urlMapping.getUpdatedAt())
                .expiryDate(urlMapping.getExpiryDate())
                .clickCount(urlMapping.getClickCount())
                .active(urlMapping.getActive())
                .isExpired(isExpired(urlMapping))
                .remainingDays(calculateRemainingDays(urlMapping.getExpiryDate()))
                .build();
    }
}