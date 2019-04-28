
ALTER TABLE stars
  ADD COLUMN wormhole_empire_id BIGINT;

CREATE INDEX stars_wormhole_empire_id ON stars (wormhole_empire_id);
