
ALTER TABLE cron
  ADD COLUMN enabled INT;

ALTER TABLE cron
  ADD COLUMN last_status TEXT;

UPDATE cron SET enabled = 1;

ALTER TABLE cron
  ALTER COLUMN enabled SET NOT NULL;
