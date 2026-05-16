package com.urlshortener.repository;

import com.urlshortener.entity.Analytics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<Analytics, Long>, JpaSpecificationExecutor<Analytics> {

    Page<Analytics> findByShortCodeOrderByClickedAtDesc(String shortCode, Pageable pageable);

    Page<Analytics> findByUrlIdOrderByClickedAtDesc(String urlId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Analytics a WHERE a.shortCode = :shortCode")
    long countByShortCode(@Param("shortCode") String shortCode);

    @Query("SELECT COUNT(a) FROM Analytics a WHERE a.urlId = :urlId")
    long countByUrlId(@Param("urlId") String urlId);

    @Query("SELECT a.browserName, COUNT(a) FROM Analytics a WHERE a.shortCode = :shortCode GROUP BY a.browserName ORDER BY COUNT(a) DESC")
    List<Object[]> findBrowserStatsByShortCode(@Param("shortCode") String shortCode);

    @Query("SELECT a.operatingSystem, COUNT(a) FROM Analytics a WHERE a.shortCode = :shortCode GROUP BY a.operatingSystem ORDER BY COUNT(a) DESC")
    List<Object[]> findOsStatsByShortCode(@Param("shortCode") String shortCode);

    @Query("SELECT a.deviceType, COUNT(a) FROM Analytics a WHERE a.shortCode = :shortCode GROUP BY a.deviceType ORDER BY COUNT(a) DESC")
    List<Object[]> findDeviceTypeStatsByShortCode(@Param("shortCode") String shortCode);

    @Query("SELECT a.country, COUNT(a) FROM Analytics a WHERE a.shortCode = :shortCode GROUP BY a.country ORDER BY COUNT(a) DESC")
    List<Object[]> findCountryStatsByShortCode(@Param("shortCode") String shortCode);

    @Query("SELECT DATE(a.clickedAt) as date, COUNT(a) FROM Analytics a WHERE a.shortCode = :shortCode AND a.clickedAt BETWEEN :startDate AND :endDate GROUP BY DATE(a.clickedAt) ORDER BY date")
    List<Object[]> findDailyStatsByShortCode(@Param("shortCode") String shortCode, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT HOUR(a.clickedAt) as hour, COUNT(a) FROM Analytics a WHERE a.shortCode = :shortCode GROUP BY HOUR(a.clickedAt) ORDER BY hour")
    List<Object[]> findHourlyStatsByShortCode(@Param("shortCode") String shortCode);

    @Query("SELECT a.referer, COUNT(a) as clickCount FROM Analytics a WHERE a.shortCode = :shortCode AND a.referer IS NOT NULL GROUP BY a.referer ORDER BY clickCount DESC")
    List<Object[]> findRefererStatsByShortCode(@Param("shortCode") String shortCode);

    @Query("SELECT a.ipAddress, COUNT(a) as clickCount FROM Analytics a WHERE a.shortCode = :shortCode GROUP BY a.ipAddress ORDER BY clickCount DESC")
    List<Object[]> findTopIpsByShortCode(@Param("shortCode") String shortCode, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT a.ipAddress) FROM Analytics a WHERE a.shortCode = :shortCode")
    long countUniqueVisitorsByShortCode(@Param("shortCode") String shortCode);

    @Modifying
    @Query("DELETE FROM Analytics a WHERE a.urlId = :urlId")
    void deleteByUrlId(@Param("urlId") String urlId);

    Page<Analytics> findByShortCodeAndClickedAtBetween(String shortCode, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}