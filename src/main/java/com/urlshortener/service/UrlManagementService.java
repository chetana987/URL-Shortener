package com.urlshortener.service;

import com.urlshortener.dto.BatchUrlRequest;
import com.urlshortener.dto.BatchUrlResponse;
import com.urlshortener.dto.UrlMappingRequest;
import com.urlshortener.dto.UrlMappingResponse;
import com.urlshortener.dto.UrlMappingUpdateRequest;
import com.urlshortener.entity.UrlMapping;
import com.urlshortener.entity.User;
import com.urlshortener.exception.CustomAliasException;
import com.urlshortener.exception.DuplicateShortCodeException;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlInactiveException;
import com.urlshortener.repository.UrlMappingRepository;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.util.Base62Encoder;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlManagementService {

    private static final Set<String> RESERVED_ALIASES = Set.of(
            "api", "admin", "www", "mail", "ftp", "localhost",
            "test", "demo", "shop", "blog", "forum",
            "v1", "v2", "v3", "oauth", "auth", "login", "logout",
            "register", "signup", "dashboard", "settings", "help"
    );

    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_-]*$");
    private static final int MIN_ALIAS_LENGTH = 3;
    private static final int MAX_ALIAS_LENGTH = 50;

    private final UrlMappingRepository urlMappingRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final RedisCacheService redisCacheService;
    private final UserRepository userRepository;

    @Value("${app.url.base-url}")
    private String baseUrl;

    @Transactional
    public UrlMappingResponse createShortUrl(UrlMappingRequest request) {
        String shortCode = generateShortCode(request);

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

        redisCacheService.cacheUrlMapping(shortCode, saved);
        redisCacheService.cacheOriginalUrl(shortCode, saved.getOriginalUrl());

        log.info("Created URL mapping: {} -> {}", shortCode, request.getOriginalUrl());

        return mapToResponse(saved);
    }

    @Transactional
    public BatchUrlResponse createBatchShortUrls(BatchUrlRequest request) {
        List<UrlMappingResponse> successfulUrls = new ArrayList<>();
        List<BatchUrlResponse.FailedUrlResponse> failedUrls = new ArrayList<>();

        for (UrlMappingRequest urlRequest : request.getUrls()) {
            try {
                UrlMappingResponse response = createShortUrl(urlRequest);
                successfulUrls.add(response);
            } catch (Exception e) {
                failedUrls.add(BatchUrlResponse.FailedUrlResponse.builder()
                        .originalUrl(urlRequest.getOriginalUrl())
                        .error(e.getMessage())
                        .build());
            }
        }

        return BatchUrlResponse.builder()
                .successCount(successfulUrls.size())
                .failureCount(failedUrls.size())
                .successfulUrls(successfulUrls)
                .failedUrls(failedUrls)
                .build();
    }

    @Transactional
    public UrlMappingResponse updateUrl(String shortCode, UrlMappingUpdateRequest request) {
        User currentUser = getCurrentUser();
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found: " + shortCode));
        verifyOwnership(urlMapping, currentUser);

        if (request.getOriginalUrl() != null) {
            urlMapping.setOriginalUrl(request.getOriginalUrl());
        }

        if (request.getCustomAlias() != null) {
            validateCustomAlias(request.getCustomAlias(), shortCode);
            urlMapping.setCustomAlias(request.getCustomAlias());
        }

        if (request.getExpiryDate() != null) {
            if (request.getExpiryDate().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Expiry date must be in the future");
            }
            urlMapping.setExpiryDate(request.getExpiryDate());
        }

        if (request.getActive() != null) {
            urlMapping.setActive(request.getActive());
            if (!request.getActive()) {
                redisCacheService.invalidateCache(shortCode);
            }
        }

        UrlMapping updated = urlMappingRepository.save(urlMapping);

        redisCacheService.refreshCache(shortCode, updated);

        log.info("Updated URL mapping: {}", shortCode);

        return mapToResponse(updated);
    }

    @Transactional
    public void deactivateUrl(String shortCode) {
        User currentUser = getCurrentUser();
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found: " + shortCode));
        verifyOwnership(urlMapping, currentUser);

        urlMapping.setActive(false);
        urlMappingRepository.save(urlMapping);

        redisCacheService.invalidateCache(shortCode);

        log.info("Deactivated URL mapping: {}", shortCode);
    }

    @Transactional
    public void activateUrl(String shortCode) {
        User currentUser = getCurrentUser();
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found: " + shortCode));
        verifyOwnership(urlMapping, currentUser);

        urlMapping.setActive(true);
        urlMappingRepository.save(urlMapping);

        redisCacheService.cacheUrlMapping(shortCode, urlMapping);
        redisCacheService.cacheOriginalUrl(shortCode, urlMapping.getOriginalUrl());

        log.info("Activated URL mapping: {}", shortCode);
    }

    @Transactional(readOnly = true)
    public UrlMappingResponse getUrlDetails(String shortCode) {
        User currentUser = getCurrentUser();
        UrlMapping urlMapping = findAndValidateUrl(shortCode);
        verifyOwnership(urlMapping, currentUser);
        return mapToResponse(urlMapping);
    }

    @Transactional(readOnly = true)
    public String resolveUrl(String shortCode) {
        UrlMapping urlMapping = findAndValidateUrl(shortCode);

        if (isExpired(urlMapping)) {
            redisCacheService.invalidateCache(shortCode);
            throw UrlExpiredException.expired(shortCode, urlMapping.getExpiryDate().toString());
        }

        return urlMapping.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public Page<UrlMappingResponse> getActiveUrls(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UrlMapping> urls = urlMappingRepository.findAllActiveUrlsByUserId(pageable, currentUser.getId(), LocalDateTime.now());
        return urls.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<UrlMappingResponse> getInactiveUrls(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<UrlMapping> urls = urlMappingRepository.findByActiveFalseByUserId(pageable, currentUser.getId());
        return urls.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<UrlMappingResponse> getExpiredUrls(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "expiryDate"));
        Page<UrlMapping> urls = urlMappingRepository.findExpiredUrlsByUserId(pageable, currentUser.getId(), LocalDateTime.now());
        return urls.map(this::mapToResponse);
    }

    @Transactional
    public void extendExpiration(String shortCode, int days) {
        User currentUser = getCurrentUser();
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found: " + shortCode));
        verifyOwnership(urlMapping, currentUser);

        LocalDateTime newExpiryDate = LocalDateTime.now().plusDays(days);
        if (urlMapping.getExpiryDate() != null && urlMapping.getExpiryDate().isAfter(newExpiryDate)) {
            newExpiryDate = urlMapping.getExpiryDate().plusDays(days);
        }

        urlMapping.setExpiryDate(newExpiryDate);
        urlMappingRepository.save(urlMapping);

        redisCacheService.refreshCache(shortCode, urlMapping);

        log.info("Extended expiration for {} by {} days", shortCode, days);
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

    private UrlMapping findAndValidateUrl(String shortCode) {
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found: " + shortCode));

        validateUrlStatus(urlMapping);

        return urlMapping;
    }

    private void validateUrlStatus(UrlMapping urlMapping) {
        if (!urlMapping.getActive()) {
            redisCacheService.invalidateCache(urlMapping.getShortCode());
            throw new UrlInactiveException("This URL has been deactivated");
        }

        if (isExpired(urlMapping)) {
            redisCacheService.invalidateCache(urlMapping.getShortCode());
            throw UrlExpiredException.expired(
                    urlMapping.getShortCode(),
                    urlMapping.getExpiryDate().toString()
            );
        }
    }

    private String generateShortCode(UrlMappingRequest request) {
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            validateCustomAlias(request.getCustomAlias(), null);
            return request.getCustomAlias().toLowerCase().trim();
        }

        String shortCode;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            shortCode = shortCodeGenerator.generate();
            attempts++;
            if (attempts > maxAttempts) {
                throw new RuntimeException("Failed to generate unique short code");
            }
        } while (urlMappingRepository.existsByShortCode(shortCode) ||
                 urlMappingRepository.existsByCustomAlias(shortCode));

        return shortCode;
    }

    private void validateCustomAlias(String alias, String excludeShortCode) {
        String normalizedAlias = alias.toLowerCase().trim();

        if (normalizedAlias.length() < MIN_ALIAS_LENGTH) {
            throw CustomAliasException.aliasInvalidFormat(alias);
        }

        if (normalizedAlias.length() > MAX_ALIAS_LENGTH) {
            throw new CustomAliasException(
                    "Custom alias must not exceed " + MAX_ALIAS_LENGTH + " characters",
                    CustomAliasException.ErrorCode.ALIAS_TOO_LONG
            );
        }

        if (!ALIAS_PATTERN.matcher(normalizedAlias).matches()) {
            throw CustomAliasException.aliasInvalidFormat(alias);
        }

        if (RESERVED_ALIASES.contains(normalizedAlias)) {
            throw CustomAliasException.aliasReserved(alias);
        }

        if (urlMappingRepository.existsByCustomAlias(normalizedAlias)) {
            throw CustomAliasException.aliasAlreadyExists(alias);
        }

        if (urlMappingRepository.existsByShortCode(normalizedAlias)) {
            throw new DuplicateShortCodeException(
                    "Custom alias '" + alias + "' conflicts with an existing short code"
            );
        }
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

    private UrlMappingResponse mapToResponse(UrlMapping urlMapping) {
        boolean expired = isExpired(urlMapping);
        Long remainingDays = null;
        if (urlMapping.getExpiryDate() != null) {
            remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), urlMapping.getExpiryDate());
            if (remainingDays < 0) remainingDays = 0L;
        }

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
                .isExpired(expired)
                .remainingDays(remainingDays)
                .build();
    }
}