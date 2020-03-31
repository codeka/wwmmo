
ALTER TABLE stars
  ADD COLUMN mod_counter INT;

UPDATE stars SET mod_counter = 1;
