
--
-- This is the first schema update that actually includes a schema_version table. So obviously
-- we have to create that table.
--

CREATE TABLE schema_version (
  version INT NOT NULL
);

-- Subsequent SQL files don't need to update the version number, it happens automatically.
INSERT INTO schema_version (version) VALUES (1);
