
ALTER TABLE empire_logins
  ADD COLUMN safetynet_attestation_statement TEXT,
  ADD COLUMN safetynet_basic_integrity INT,
  ADD COLUMN safetynet_cts_profile INT;
