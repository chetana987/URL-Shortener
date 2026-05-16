package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "click_logs", indexes = {
        @Index(name = "idx_url_id", columnList = "urlId"),
        @Index(name = "idx_clicked_at", columnList = "clickedAt")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class ClickLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String urlId;

    @Column(nullable = false)
    private String shortCode;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 512)
    private String userAgent;

    @Column(length = 100)
    private String referer;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime clickedAt;
}
