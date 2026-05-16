package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "analytics",
        indexes = {
                @Index(name = "idx_url_id", columnList = "urlId"),
                @Index(name = "idx_short_code", columnList = "shortCode"),
                @Index(name = "idx_clicked_at", columnList = "clickedAt"),
                @Index(name = "idx_ip_address", columnList = "ipAddress"),
                @Index(name = "idx_country", columnList = "country"),
                @Index(name = "idx_device_type", columnList = "deviceType")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class Analytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String urlId;

    @Column(nullable = false, length = 20)
    private String shortCode;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 512)
    private String userAgent;

    @Column(length = 100)
    private String referer;

    @Column(length = 50)
    private String browserName;

    @Column(length = 50)
    private String browserVersion;

    @Column(length = 50)
    private String operatingSystem;

    @Column(length = 20)
    private String deviceType;

    @Column(length = 10)
    private String deviceBrand;

    @Column(length = 50)
    private String country;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String region;

    @Column(precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitude;

    @Column(length = 50)
    private String isp;

    @Column(length = 10)
    private String httpMethod;

    @Column(length = 50)
    private String responseStatus;

    @Column(length = 255)
    private String queryParams;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime clickedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "urlId", insertable = false, updatable = false)
    @ToString.Exclude
    private UrlMapping urlMapping;
}