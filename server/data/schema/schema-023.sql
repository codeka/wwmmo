
ALTER TABLE devices
  ADD COLUMN deny_access INT,
  ADD COLUMN deny_date TIMESTAMP WITH TIME ZONE,
  ADD COLUMN deny_reason TEXT
