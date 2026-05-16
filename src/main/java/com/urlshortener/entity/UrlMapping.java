package com.urlshortener.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "url_mappings",
        indexes = {
                @Index(name = "idx_short_code", columnList = "shortCode", unique = true),
                @Index(name = "idx_custom_alias", columnList = "customAlias"),
                @Index(name = "idx_active", columnList = "active"),
                @Index(name = "idx_expiry_date", columnList = "expiryDate"),
                @Index(name = "idx_created_at", columnList = "createdAt")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    @EqualsAndHashCode.Include
    private String id;

    @NotBlank(message = "Original URL is required")
    @Size(max = 2048, message = "Original URL must not exceed 2048 characters")
    @Column(nullable = false, length = 2048)
    @ToString.Exclude
    private String originalUrl;

    @NotBlank(message = "Short code is required")
    @Size(min = 3, max = 20, message = "Short code must be between 3 and 20 characters")
    @Column(nullable = false, unique = true, length = 20)
    private String shortCode;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime expiryDate;

    @Builder.Default
    @Column(nullable = false)
    private Long clickCount = 0L;

    @Size(max = 50, message = "Custom alias must not exceed 50 characters")
    @Column(length = 50)
    @ToString.Exclude
    private String customAlias;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;
}
