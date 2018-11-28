
-- This table contains all the details from users who've linked their patreon accounts to their
-- empire.
CREATE TABLE patreon (
  id BIGSERIAL NOT NULL,
  empire_id BIGINT NOT NULL,
  access_token TEXT NOT NULL,
  refresh_token TEXT NOT NULL,
  token_type TEXT NOT NULL,
  token_scope TEXT NOT NULL,
  token_expiry_time BIGINT NOT NULL,
  patreon_url TEXT,
  full_name TEXT,
  discord_id TEXT,
  about TEXT,
  image_url TEXT,
  email TEXT,
  max_pledge INT
);

CREATE UNIQUE INDEX patreon_empires ON patreon (empire_id);
