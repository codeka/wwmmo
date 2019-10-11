
ALTER TABLE cron
  ADD COLUMN run_once INT;

UPDATE cron SET run_once = 0;

ALTER TABLE cron
  ALTER COLUMN run_once SET NOT NULL;
