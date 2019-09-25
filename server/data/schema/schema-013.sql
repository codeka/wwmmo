
DROP TABLE chat_sinbin;

CREATE TABLE chat_blocked (
  empire_id BIGINT NOT NULL,
  blocked_empire_id BIGINT NOT NULL,
  created_date TIMESTAMP WITH TIME ZONE
);

ALTER TABLE chat_blocked
  ADD CONSTRAINT fk_chat_blocked_empires FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE chat_blocked
  ADD CONSTRAINT fk_chat_blocked_blocked_empires FOREIGN KEY (blocked_empire_id) REFERENCES empires(id);
