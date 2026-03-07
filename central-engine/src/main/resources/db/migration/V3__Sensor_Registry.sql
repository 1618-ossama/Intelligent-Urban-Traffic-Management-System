-- ═══════════════════════════════════════════════════════════════
-- IUTMS - Sensor Registry Migration
-- ═══════════════════════════════════════════════════════════════

-- Extend alert_type enum to support sensor liveness alerts
ALTER TABLE alerts
  MODIFY COLUMN alert_type
    ENUM('CONGESTION','POLLUTION','ACCIDENT','NOISE','RUSH_HOUR','SENSOR_STALE') NOT NULL;

-- Sensor self-registration registry
CREATE TABLE IF NOT EXISTS sensors (
  id            VARCHAR(64)  PRIMARY KEY,
  sensor_type   ENUM('NOISE','POLLUTION','CAMERA','FLUX','SIGNAL') NOT NULL,
  zone_id       VARCHAR(50)  NOT NULL,
  service_host  VARCHAR(128) NOT NULL,
  service_port  INT          NOT NULL,
  protocol      ENUM('TCP','REST','RMI','SOAP_WS','SOAP_RPC') NOT NULL,
  status        ENUM('ACTIVE','STALE','DEREGISTERED') NOT NULL DEFAULT 'ACTIVE',
  registered_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_heartbeat DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  metadata      JSON,
  FOREIGN KEY (zone_id) REFERENCES zones(id),
  INDEX idx_sensor_zone   (zone_id),
  INDEX idx_sensor_status (status)
) ENGINE=InnoDB;
