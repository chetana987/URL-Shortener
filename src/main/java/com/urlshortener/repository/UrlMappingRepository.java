package com.urlshortener.repository;

import com.urlshortener.entity.UrlMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, String> {

    Optional<UrlMapping> findByShortCode(String shortCode);

    Optional<UrlMapping> findByShortCodeAndActiveTrue(String shortCode);

    Optional<UrlMapping> findByCustomAlias(String customAlias);

    boolean existsByShortCode(String shortCode);

    boolean existsByCustomAlias(String customAlias);

    Page<UrlMapping> findByActiveTrue(Pageable pageable);

    Page<UrlMapping> findByActiveTrueAndExpiryDateGreaterThan(Pageable pageable, LocalDateTime now);

    Page<UrlMapping> findByActiveTrueAndExpiryDateIsNull(Pageable pageable);

    @Query("SELECT u FROM UrlMapping u WHERE u.user.id = :userId AND u.active = true AND (u.expiryDate IS NULL OR u.expiryDate > :now)")
    Page<UrlMapping> findAllActiveUrlsByUserId(Pageable pageable, @Param("userId") String userId, @Param("now") LocalDateTime now);

    @Query("SELECT u FROM UrlMapping u WHERE u.user.id = :userId AND u.active = false")
    Page<UrlMapping> findByActiveFalseByUserId(Pageable pageable, @Param("userId") String userId);

    @Query("SELECT u FROM UrlMapping u WHERE u.user.id = :userId AND u.expiryDate IS NOT NULL AND u.expiryDate < :now")
    Page<UrlMapping> findExpiredUrlsByUserId(Pageable pageable, @Param("userId") String userId, @Param("now") LocalDateTime now);

    @Query("SELECT u FROM UrlMapping u WHERE u.active = true AND (u.expiryDate IS NULL OR u.expiryDate > :now)")
    Page<UrlMapping> findAllActiveNonExpired(Pageable pageable, @Param("now") LocalDateTime now);

    @Query("SELECT u FROM UrlMapping u WHERE u.active = true AND (u.expiryDate IS NULL OR u.expiryDate > :now)")
    Page<UrlMapping> findAllActiveUrls(Pageable pageable, @Param("now") LocalDateTime now);

    Page<UrlMapping> findByActiveFalse(Pageable pageable);

    @Query("SELECT u FROM UrlMapping u WHERE u.expiryDate IS NOT NULL AND u.expiryDate < :now")
    Page<UrlMapping> findExpiredUrls(Pageable pageable, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.active = false WHERE u.shortCode = :shortCode")
    void deactivateByShortCode(@Param("shortCode") String shortCode);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.active = false WHERE u.expiryDate < :now AND u.active = true")
    int deactivateExpiredUrls(@Param("now") LocalDateTime now);

    List<UrlMapping> findByExpiryDateBeforeAndActiveTrue(LocalDateTime dateTime);

    @Query("SELECT COUNT(u) FROM UrlMapping u WHERE u.active = true")
    long countActiveUrls();

    @Query("SELECT COUNT(u) FROM UrlMapping u WHERE u.active = false")
    long countInactiveUrls();

    @Query("SELECT COUNT(u) FROM UrlMapping u WHERE u.expiryDate IS NOT NULL AND u.expiryDate < :now")
    long countExpiredUrls(@Param("now") LocalDateTime now);

    boolean existsByShortCodeAndActiveTrue(String shortCode);

    @Query("SELECT u FROM UrlMapping u WHERE u.active = true AND (u.expiryDate IS NULL OR u.expiryDate > :now) ORDER BY u.createdAt DESC")
    Page<UrlMapping> findRecentActiveUrls(Pageable pageable, @Param("now") LocalDateTime now);

    @Query("SELECT u FROM UrlMapping u WHERE u.active = true AND (u.expiryDate IS NULL OR u.expiryDate > :now) ORDER BY u.clickCount DESC")
    Page<UrlMapping> findPopularUrls(Pageable pageable, @Param("now") LocalDateTime now);
}
