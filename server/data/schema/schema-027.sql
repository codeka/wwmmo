
ALTER TABLE error_reports
  ADD COLUMN source INT;

UPDATE error_reports
SET source=1
WHERE CONTEXT LIKE '/%';

UPDATE error_reports
SET source=2
WHERE source IS null;

