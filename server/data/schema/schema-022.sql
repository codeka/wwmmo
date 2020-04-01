

CREATE TABLE accessibility_services (
  name TEXT NOT NULL,
  display_name TEXT,
  empires TEXT NOT NULL,
  first_seen TIMESTAMP WITH TIME ZONE NOT NULL,
  last_seen TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IX_accessibility_services_name ON accessibility_services (name);
