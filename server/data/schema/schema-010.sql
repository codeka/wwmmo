
ALTER TABLE stars
  ADD COLUMN wormhole_empire_id BIGINT;

CREATE INDEX stars_wormhole_star_type_empire_id ON stars (star_type, wormhole_empire_id);
