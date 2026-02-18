-- ═══════════════════════════════════════════════════════════════
-- IUTMS - Intelligent Urban Traffic Management System
-- Database Schema
-- ═══════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS iutms_db;
USE iutms_db;

-- Vehicle flow sensor readings
CREATE TABLE IF NOT EXISTS vehicle_flow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    road_id VARCHAR(50) NOT NULL,
    zone_id VARCHAR(50) NOT NULL,
    vehicle_count INT NOT NULL,
    flow_rate DOUBLE NOT NULL COMMENT 'vehicles per minute',
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_flow_zone (zone_id, recorded_at),
    INDEX idx_flow_road (road_id, recorded_at)
) ENGINE=InnoDB;

-- Pollution sensor readings
CREATE TABLE IF NOT EXISTS pollution_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    zone_id VARCHAR(50) NOT NULL,
    co2_level DOUBLE NOT NULL,
    nox_level DOUBLE NOT NULL,
    pm25_level DOUBLE NOT NULL,
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pollution_zone (zone_id, recorded_at)
) ENGINE=InnoDB;

-- Camera event detections
CREATE TABLE IF NOT EXISTS camera_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_id VARCHAR(50) NOT NULL,
    zone_id VARCHAR(50) NOT NULL,
    event_type ENUM('NORMAL','ACCIDENT','ANOMALY','CONGESTION') NOT NULL DEFAULT 'NORMAL',
    severity ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'LOW',
    description TEXT,
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_camera_zone (zone_id, recorded_at),
    INDEX idx_camera_type (event_type, recorded_at)
) ENGINE=InnoDB;

-- Noise sensor readings
CREATE TABLE IF NOT EXISTS noise_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    zone_id VARCHAR(50) NOT NULL,
    decibel_level DOUBLE NOT NULL,
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_noise_zone (zone_id, recorded_at)
) ENGINE=InnoDB;

-- Traffic signal states
CREATE TABLE IF NOT EXISTS signal_states (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    intersection_id VARCHAR(50) NOT NULL,
    zone_id VARCHAR(50) NOT NULL,
    color ENUM('RED','GREEN','YELLOW') NOT NULL,
    duration_sec INT NOT NULL,
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_signal_zone (zone_id, recorded_at)
) ENGINE=InnoDB;

-- Generated alerts
CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_type ENUM('CONGESTION','POLLUTION','ACCIDENT','NOISE','RUSH_HOUR') NOT NULL,
    zone_id VARCHAR(50) NOT NULL,
    severity ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
    message TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME,
    INDEX idx_alert_active (is_active, created_at),
    INDEX idx_alert_zone (zone_id, created_at)
) ENGINE=InnoDB;

-- System recommendations
CREATE TABLE IF NOT EXISTS recommendations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id BIGINT,
    action_type ENUM('EXTEND_GREEN','REDUCE_TRAFFIC','DEVIATE','SYNC_SIGNALS','OPEN_LANE','PRIORITIZE_PUBLIC') NOT NULL,
    description TEXT NOT NULL,
    status ENUM('PENDING','APPLIED','DISMISSED') NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_at DATETIME,
    FOREIGN KEY (alert_id) REFERENCES alerts(id),
    INDEX idx_rec_status (status, created_at)
) ENGINE=InnoDB;
