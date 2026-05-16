package com.urlshortener.repository;

import com.urlshortener.entity.ClickLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClickLogRepository extends JpaRepository<ClickLog, Long> {

    Page<ClickLog> findByUrlIdOrderByClickedAtDesc(String urlId, Pageable pageable);

    long countByUrlId(String urlId);
}
