-- ═══════════════════════════════════════════════════════════════
-- IUTMS - Seed Data for Testing
-- ═══════════════════════════════════════════════════════════════
USE iutms_db;

-- Sample signal initial states
INSERT INTO signal_states (intersection_id, zone_id, color, duration_sec) VALUES
('INT-01', 'zone-center', 'GREEN', 30),
('INT-02', 'zone-center', 'RED', 30),
('INT-03', 'zone-center', 'GREEN', 45),
('INT-04', 'zone-north', 'RED', 30),
('INT-05', 'zone-north', 'GREEN', 30),
('INT-06', 'zone-south', 'YELLOW', 5);
