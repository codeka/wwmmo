
-- This table contains a list of user who have access to the backend website.
CREATE TABLE backend_users (
  id BIGSERIAL NOT NULL,
  name VARCHAR(200) NOT NULL,
  email VARCHAR(100) NOT NULL,
  roles TEXT NOT NULL, -- comma separated list of roles, as defined in BackendUser.Role.
  last_login TIMESTAMP WITH TIME ZONE -- can be null if never logged in
);
