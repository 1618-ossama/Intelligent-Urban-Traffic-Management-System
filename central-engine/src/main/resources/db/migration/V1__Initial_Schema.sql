-- ═══════════════════════════════════════════════════════════════
-- IUTMS - Intelligent Urban Traffic Management System
-- Database Schema - Initial Migration
-- ═══════════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════════
-- DIMENSION TABLES (Master Data)
-- ═══════════════════════════════════════════════════════════════

-- Zones/Areas in the city
CREATE TABLE IF NOT EXISTS zones (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    area_sq_km DOUBLE,
    description TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_zone_name (name)
) ENGINE=InnoDB;

-- Traffic intersections
CREATE TABLE IF NOT EXISTS intersections (
    id VARCHAR(50) PRIMARY KEY,
    zone_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    num_lanes INT,
    description TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (zone_id) REFERENCES zones(id),
    INDEX idx_intersection_zone (zone_id),
    INDEX idx_intersection_name (name)
) ENGINE=InnoDB;

-- Traffic camera sensors
CREATE TABLE IF NOT EXISTS cameras (
    id VARCHAR(50) PRIMARY KEY,
    zone_id VARCHAR(50) NOT NULL,
    intersection_id VARCHAR(50),
    camera_name VARCHAR(100) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    camera_type ENUM('VEHICLE_DETECTION','ANOMALY_DETECTION','GENERAL_MONITORING') NOT NULL,
    status ENUM('ACTIVE','INACTIVE','MAINTENANCE') NOT NULL DEFAULT 'ACTIVE',
    last_heartbeat DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (zone_id) REFERENCES zones(id),
    FOREIGN KEY (intersection_id) REFERENCES intersections(id),
    INDEX idx_camera_zone (zone_id),
    INDEX idx_camera_status (status)
) ENGINE=InnoDB;

-- ═══════════════════════════════════════════════════════════════
-- FACT TABLES (Time-Series Data)
-- ═══════════════════════════════════════════════════════════════

-- Vehicle flow sensor readings
CREATE TABLE IF NOT EXISTS vehicle_flow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    road_id VARCHAR(50) NOT NULL,
    zone_id VARCHAR(50) NOT NULL,
    vehicle_count INT NOT NULL CHECK (vehicle_count >= 0),
    flow_rate DOUBLE NOT NULL CHECK (flow_rate >= 0) COMMENT 'vehicles per minute',
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_flow_zone (zone_id, recorded_at),
    INDEX idx_flow_road (road_id, recorded_at)
) ENGINE=InnoDB;

-- Pollution sensor readings
CREATE TABLE IF NOT EXISTS pollution_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    zone_id VARCHAR(50) NOT NULL,
    co2_level DOUBLE NOT NULL CHECK (co2_level >= 0),
    nox_level DOUBLE NOT NULL CHECK (nox_level >= 0),
    pm25_level DOUBLE NOT NULL CHECK (pm25_level >= 0),
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
    FOREIGN KEY (camera_id) REFERENCES cameras(id),
    INDEX idx_camera_type (event_type, recorded_at),
    INDEX idx_camera_severity (severity, recorded_at)
) ENGINE=InnoDB;

-- Noise sensor readings
CREATE TABLE IF NOT EXISTS noise_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    zone_id VARCHAR(50) NOT NULL,
    decibel_level DOUBLE NOT NULL CHECK (decibel_level >= 0),
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (zone_id) REFERENCES zones(id),
    INDEX idx_noise_zone (zone_id, recorded_at)
) ENGINE=InnoDB;

-- Traffic signal states
CREATE TABLE IF NOT EXISTS signal_states (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    intersection_id VARCHAR(50) NOT NULL,
    zone_id VARCHAR(50) NOT NULL,
    color ENUM('RED','GREEN','YELLOW') NOT NULL,
    duration_sec INT NOT NULL CHECK (duration_sec > 0),
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (intersection_id) REFERENCES intersections(id),
    INDEX idx_signal_intersection (intersection_id, recorded_at)
) ENGINE=InnoDB;

-- Generated alerts
CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_type ENUM('CONGESTION','POLLUTION','ACCIDENT','NOISE','RUSH_HOUR') NOT NULL,
    zone_id VARCHAR(50) NOT NULL,
    severity ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
    message TEXT NOT NULL,
    percent_of_threshold DOUBLE NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME,
    FOREIGN KEY (zone_id) REFERENCES zones(id),
    INDEX idx_alert_active (is_active, created_at),
    INDEX idx_alert_severity (severity, created_at)
) ENGINE=InnoDB;

-- System recommendations
CREATE TABLE IF NOT EXISTS recommendations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id BIGINT NOT NULL,
    action_type ENUM('EXTEND_GREEN','REDUCE_TRAFFIC','DEVIATE','SYNC_SIGNALS','OPEN_LANE','PRIORITIZE_PUBLIC') NOT NULL,
    description TEXT NOT NULL,
    status ENUM('PENDING','APPLIED','DISMISSED') NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_at DATETIME,
    FOREIGN KEY (alert_id) REFERENCES alerts(id) ON DELETE CASCADE,
    INDEX idx_rec_status (status, created_at),
    INDEX idx_rec_alert (alert_id)
) ENGINE=InnoDB;
