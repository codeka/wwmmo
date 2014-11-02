
-- Swap the order of empire_count and last_simulation in this index, it makes allows
-- the "next star to simulate" query do a single index scan.
DROP INDEX IF EXISTS idx_30697_ix_stars_last_simulation;
DROP INDEX IF EXISTS ix_stars_last_simulation_empire_count;
CREATE INDEX ix_stars_last_simulation_empire_count ON stars (last_simulation, empire_count, id);
