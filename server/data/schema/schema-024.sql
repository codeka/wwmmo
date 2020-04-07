
ALTER TABLE empire_logins
  ADD COLUMN success INT,
  ADD COLUMN failure_reason TEXT;
