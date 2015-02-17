
-- This table contains all the IP address each empire has logged in from.
CREATE TABLE empire_ips (
  empire_id BIGINT NOT NULL,
  ip_address VARCHAR(50) NOT NULL,
  last_seen TIMESTAMP WITH TIME ZONE -- The date/time we last saw this empire at this IP
);

CREATE UNIQUE INDEX idx_empire_ips ON empire_ips (empire_id, ip_address);
