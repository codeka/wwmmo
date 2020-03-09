
ALTER TABLE sessions
  ADD COLUMN client_id TEXT;

ALTER TABLE empire_logins
  ADD COLUMN client_id TEXT;
