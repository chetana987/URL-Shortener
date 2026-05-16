-- URL Shortener Database Schema
-- MySQL 8.x Compatible

CREATE DATABASE IF NOT EXISTS url_shortener;
USE url_shortener;

-- Drop existing tables if needed (for fresh setup)
-- DROP TABLE IF EXISTS click_logs;
-- DROP TABLE IF EXISTS url_mappings;
-- DROP TABLE IF EXISTS user_roles;
-- DROP TABLE IF EXISTS roles;
-- DROP TABLE IF EXISTS users;

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Roles Table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User Roles Join Table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id VARCHAR(36) NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- URL Mappings Table
CREATE TABLE IF NOT EXISTS url_mappings (
    id VARCHAR(36) PRIMARY KEY,
    original_url VARCHAR(2048) NOT NULL,
    short_code VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    expiry_date TIMESTAMP NULL,
    click_count BIGINT NOT NULL DEFAULT 0,
    custom_alias VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_short_code (short_code),
    INDEX idx_custom_alias (custom_alias),
    INDEX idx_active (active),
    INDEX idx_expiry_date (expiry_date),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Click Logs Table
CREATE TABLE IF NOT EXISTS click_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url_id VARCHAR(36) NOT NULL,
    short_code VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    referer VARCHAR(100),
    clicked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_url_id (url_id),
    INDEX idx_clicked_at (clicked_at),
    FOREIGN KEY (url_id) REFERENCES url_mappings(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default roles
INSERT INTO roles (name) VALUES ('USER'), ('ADMIN') ON DUPLICATE KEY UPDATE name = name;
