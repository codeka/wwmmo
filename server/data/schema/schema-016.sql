
CREATE TABLE game_history (
  id BIGSERIAL NOT NULL,
  date_created TIMESTAMP WITH TIME ZONE NOT NULL,
  date_finished TIMESTAMP WITH TIME ZONE,
  state INT NOT NULL
);

INSERT INTO game_history (date_created, state) VALUES ('2019-09-16 12:00:00', 0);

ALTER TABLE ONLY game_history
    ADD CONSTRAINT game_history_pkey PRIMARY KEY (id);

ALTER TABLE empire_rank_histories
  DROP COLUMN date;

ALTER TABLE empire_rank_histories
  ADD COLUMN game_history_id INT NOT NULL;

ALTER TABLE empire_rank_histories
  ADD CONSTRAINT fk_empire_rank_histories_game_history
     FOREIGN KEY (game_history_id) REFERENCES game_history(id);
