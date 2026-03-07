-- Integration test schema for MySQLContainer
-- Simplified (no PARTITION BY RANGE) to avoid MySQL FK/partition compatibility issues

CREATE TABLE IF NOT EXISTS zones (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    latitude DOUBLE NOT NULL DEFAULT 0.0,
    longitude DOUBLE NOT NULL DEFAULT 0.0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS noise_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    zone_id VARCHAR(50) NOT NULL,
    decibel_level DOUBLE NOT NULL,
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (zone_id) REFERENCES zones(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_type VARCHAR(50) NOT NULL,
    zone_id VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    percent_of_threshold DOUBLE NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME,
    FOREIGN KEY (zone_id) REFERENCES zones(id)
) ENGINE=InnoDB;

-- Seed test zone required by FK constraints on noise_data and alerts
INSERT INTO zones (id, name, latitude, longitude)
VALUES ('zone-test', 'Test Zone', 48.8566, 2.3522);
