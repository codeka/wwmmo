
-- Add a flag to indicate whether the alliance is active or not. Inactive alliances are filtered
-- out of search results by default. An inactive alliance is one where nobody has logged in for a
-- while.
ALTER TABLE alliances
  ADD COLUMN is_active INTEGER;

-- Everybody's active by default (until a cron job runs and resets it).
UPDATE alliances SET is_active = 1;

-- Add a user-editable description for their alliance.
ALTER TABLE alliances
  ADD COLUMN description TEXT;

-- Add a column for the total number of stars under the alliance's control.
ALTER TABLE alliances
  ADD COLUMN total_stars BIGINT;
