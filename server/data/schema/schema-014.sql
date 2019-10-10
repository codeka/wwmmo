
CREATE TABLE empire_resets (
  empire_id BIGINT NOT NULL,
  reset_reason TEXT NOT NULL,
  reset_date TIMESTAMP WITH TIME ZONE
);

ALTER TABLE empire_resets
  ADD CONSTRAINT fk_empire_resets_empires FOREIGN KEY (empire_id) REFERENCES empires(id);

