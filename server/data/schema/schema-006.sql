
-- Add a flag to indicate whether the alliance is active or not. Inactive alliances are filtered
-- out of search results by default. An inactive alliance is one where nobody has logged in for a
-- while.
ALTER TABLE alliances
  ADD COLUMN is_active INTEGER;

-- Everybody's active by default (until a cron job runs and resets it).
UPDATE alliances SET is_active = 1;
