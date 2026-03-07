-- ═══════════════════════════════════════════════════════════════
-- IUTMS - Zone Registry Extension Migration
-- Adds partition_idx, radius_m, is_active to zones table.
-- The existing latitude/longitude columns provide geo coordinates.
-- ═══════════════════════════════════════════════════════════════

ALTER TABLE zones
  ADD COLUMN radius_m      INT     NOT NULL DEFAULT 500   AFTER description,
  ADD COLUMN partition_idx INT     NOT NULL DEFAULT 0     AFTER radius_m,
  ADD COLUMN is_active     BOOLEAN NOT NULL DEFAULT TRUE  AFTER partition_idx;

-- Seed partition affinity and boundary radius for the 6 seeded zones.
-- Partition assignments: 0=Engine-1(center,east), 1=Engine-2(north,west), 2=Engine-3(south,industrial)
UPDATE zones SET radius_m = 600, partition_idx = 0 WHERE id = 'zone-center';
UPDATE zones SET radius_m = 500, partition_idx = 1 WHERE id = 'zone-north';
UPDATE zones SET radius_m = 500, partition_idx = 2 WHERE id = 'zone-south';
UPDATE zones SET radius_m = 500, partition_idx = 0 WHERE id = 'zone-east';
UPDATE zones SET radius_m = 500, partition_idx = 1 WHERE id = 'zone-west';
UPDATE zones SET radius_m = 700, partition_idx = 2 WHERE id = 'zone-industrial';
