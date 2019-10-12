
CREATE TABLE game_history (
  id BIGSERIAL NOT NULL,
  date_created TIMESTAMP WITH TIME ZONE NOT NULL,
  state INT NOT NULL
);

INSERT INTO game_history (date_created, state) VALUES ('2019-09-16 12:00:00', 0);

ALTER TABLE ONLY game_history
    ADD CONSTRAINT game_history_pkey PRIMARY KEY (id);
