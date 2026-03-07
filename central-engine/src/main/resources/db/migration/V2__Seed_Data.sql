-- ═══════════════════════════════════════════════════════════════
-- IUTMS - Seed Data for Initial Setup
-- ═══════════════════════════════════════════════════════════════

-- Sample zones
INSERT INTO zones (id, name, latitude, longitude, area_sq_km, description) VALUES
('zone-center', 'Downtown Center', 48.8566, 2.3522, 15.5, 'Central business district'),
('zone-north', 'North District', 48.8800, 2.3600, 22.3, 'Northern residential zone'),
('zone-south', 'South District', 48.8300, 2.3450, 18.7, 'Southern industrial zone'),
('zone-east', 'East District', 48.8666, 2.3800, 19.2, 'Eastern commercial zone'),
('zone-west', 'West District', 48.8466, 2.3222, 17.8, 'Western residential zone'),
('zone-industrial', 'Industrial Zone', 48.8200, 2.3600, 25.5, 'Industrial complex');

-- Sample intersections
INSERT INTO intersections (id, zone_id, name, latitude, longitude, num_lanes, description) VALUES
('INT-01', 'zone-center', 'Central Square', 48.8566, 2.3522, 4, 'Main central intersection'),
('INT-02', 'zone-center', 'Market District', 48.8580, 2.3540, 3, 'Market area intersection'),
('INT-03', 'zone-north', 'North Avenue', 48.8800, 2.3600, 4, 'Main northern avenue'),
('INT-04', 'zone-north', 'Park Crossing', 48.8820, 2.3620, 2, 'Park area crossing'),
('INT-05', 'zone-south', 'Industrial Way', 48.8300, 2.3450, 5, 'Industrial zone crossing'),
('INT-06', 'zone-south', 'Station Hub', 48.8270, 2.3470, 6, 'Railway station interchange'),
('INT-07', 'zone-east', 'East Avenue', 48.8666, 2.3800, 4, 'Eastern business avenue'),
('INT-08', 'zone-west', 'West Market', 48.8466, 2.3222, 3, 'Western market intersection');

-- Sample cameras (IDs match GeographyConfig defaults: cam-01..cam-06)
INSERT INTO cameras (id, zone_id, intersection_id, camera_name, latitude, longitude, camera_type, status) VALUES
('cam-01', 'zone-center', 'INT-01', 'Central Square Camera', 48.8566, 2.3522, 'VEHICLE_DETECTION', 'ACTIVE'),
('cam-02', 'zone-center', 'INT-02', 'Market District Camera', 48.8580, 2.3540, 'ANOMALY_DETECTION', 'ACTIVE'),
('cam-03', 'zone-north', 'INT-03', 'North Avenue Camera', 48.8800, 2.3600, 'VEHICLE_DETECTION', 'ACTIVE'),
('cam-04', 'zone-north', 'INT-04', 'Park Crossing Camera', 48.8820, 2.3620, 'GENERAL_MONITORING', 'ACTIVE'),
('cam-05', 'zone-south', 'INT-05', 'Industrial Way Camera', 48.8300, 2.3450, 'VEHICLE_DETECTION', 'ACTIVE'),
('cam-06', 'zone-south', 'INT-06', 'Station Hub Camera', 48.8270, 2.3470, 'GENERAL_MONITORING', 'ACTIVE');

-- Sample signal initial states
INSERT INTO signal_states (intersection_id, zone_id, color, duration_sec) VALUES
('INT-01', 'zone-center', 'GREEN', 30),
('INT-02', 'zone-center', 'RED', 30),
('INT-03', 'zone-north', 'GREEN', 45),
('INT-04', 'zone-north', 'RED', 30),
('INT-05', 'zone-south', 'GREEN', 30),
('INT-06', 'zone-south', 'YELLOW', 5),
('INT-07', 'zone-east', 'RED', 30),
('INT-08', 'zone-west', 'GREEN', 40);
