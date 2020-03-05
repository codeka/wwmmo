
ALTER TABLE empire_logins
  ADD COLUMN num_accessibility_services INT;

ALTER TABLE empire_logins
  ADD COLUMN accessibility_service_infos BYTEA;
