
CREATE TABLE empire_battle_ranks (
    empire_id bigint NOT NULL,
    day int NOT NULL,
    ships_destroyed bigint NOT NULL,
    population_destroyed bigint NOT NULL,
    colonies_destroyed bigint NOT NULL
);

CREATE UNIQUE INDEX IX_empire_battle_ranks_empire_day ON empire_battle_ranks (empire_id, day);
