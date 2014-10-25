--
-- This file is just a dump of the database as it existed October 25th when the schema feature was
-- added to War Worlds. It will re-create the database so that matches the original state.
-- Subsequent schema-nnn.sql files are used to ensure the database is kept up-to-date.
--

CREATE TABLE abandoned_stars (
    id bigserial NOT NULL,
    star_id bigint NOT NULL,
    empire_id bigint NOT NULL,
    distance_to_centre double precision NOT NULL,
    distance_to_non_abandoned_empire double precision NOT NULL
);

CREATE TABLE alliance_bank_balance_audit (
    id bigserial NOT NULL,
    alliance_id bigint NOT NULL,
    alliance_request_id bigint NOT NULL,
    empire_id bigint NOT NULL,
    date timestamp with time zone,
    amount_before double precision NOT NULL,
    amount_after double precision NOT NULL
);

CREATE TABLE alliance_join_requests (
    id bigserial NOT NULL,
    alliance_id bigint NOT NULL,
    empire_id bigint NOT NULL,
    request_date timestamp with time zone,
    message text,
    state bigint NOT NULL
);

CREATE TABLE alliance_request_votes (
    id bigserial NOT NULL,
    alliance_id bigint NOT NULL,
    alliance_request_id bigint NOT NULL,
    empire_id bigint NOT NULL,
    votes bigint NOT NULL,
    date timestamp with time zone
);

CREATE TABLE alliance_requests (
    id bigserial NOT NULL,
    alliance_id bigint NOT NULL,
    request_empire_id bigint NOT NULL,
    request_date timestamp with time zone,
    request_type bigint NOT NULL,
    message text,
    state bigint NOT NULL,
    votes bigint NOT NULL,
    target_empire_id bigint,
    amount double precision,
    png_image bytea,
    new_name text
);

CREATE TABLE alliance_shields (
    id bigserial NOT NULL,
    alliance_id bigint NOT NULL,
    create_date timestamp with time zone,
    image bytea
);

CREATE TABLE alliances (
    id bigserial NOT NULL,
    name text NOT NULL,
    creator_empire_id bigint NOT NULL,
    created_date timestamp with time zone,
    bank_balance double precision NOT NULL,
    image_updated_date timestamp with time zone
);

CREATE TABLE build_requests (
    id bigserial NOT NULL,
    star_id bigint NOT NULL,
    planet_index bigint NOT NULL,
    colony_id bigint NOT NULL,
    empire_id bigint NOT NULL,
    existing_building_id bigint,
    design_kind bigint NOT NULL,
    design_id text NOT NULL,
    existing_fleet_id bigint,
    upgrade_id text,
    count bigint NOT NULL,
    progress double precision NOT NULL,
    processing bigint NOT NULL,
    start_time timestamp with time zone,
    end_time timestamp with time zone,
    disable_notification bigint,
    notes text
);

CREATE TABLE buildings (
    id bigserial NOT NULL,
    colony_id bigint NOT NULL,
    star_id bigint NOT NULL,
    empire_id bigint,
    design_id text NOT NULL,
    build_time timestamp with time zone,
    level bigint NOT NULL,
    notes text
);

CREATE TABLE chat_abuse_reports (
    id bigserial NOT NULL,
    chat_msg_id bigint NOT NULL,
    empire_id bigint NOT NULL,
    reporting_empire_id bigint NOT NULL,
    reported_date timestamp with time zone
);

CREATE TABLE chat_conversation_participants (
    id bigserial NOT NULL,
    conversation_id bigint NOT NULL,
    empire_id bigint NOT NULL,
    is_muted smallint NOT NULL
);

CREATE TABLE chat_conversations (
    id bigserial NOT NULL
);

CREATE TABLE chat_messages (
    id bigserial NOT NULL,
    empire_id bigint,
    alliance_id bigint,
    message text NOT NULL,
    message_en text,
    profanity_level bigint,
    posted_date timestamp with time zone,
    conversation_id bigint,
    action bigint
);

CREATE TABLE chat_profane_words (
    profanity_level bigint NOT NULL,
    words text NOT NULL
);

CREATE TABLE chat_sinbin (
    id bigserial NOT NULL,
    empire_id bigint NOT NULL,
    expiry timestamp with time zone
);

CREATE TABLE colonies (
    id bigserial NOT NULL,
    sector_id bigint NOT NULL,
    star_id bigint NOT NULL,
    planet_index bigint NOT NULL,
    empire_id bigint,
    focus_population double precision NOT NULL,
    focus_construction double precision NOT NULL,
    focus_farming double precision NOT NULL,
    focus_mining double precision NOT NULL,
    population double precision NOT NULL,
    uncollected_taxes double precision NOT NULL,
    cooldown_end_time timestamp with time zone
);

CREATE TABLE combat_reports (
    id bigserial NOT NULL,
    star_id bigint NOT NULL,
    start_time timestamp with time zone,
    end_time timestamp with time zone,
    rounds bytea NOT NULL
);

CREATE TABLE dashboard_stats (
    date date NOT NULL,
    active_1d bigint NOT NULL,
    active_7d bigint NOT NULL,
    new_signups bigint NOT NULL
);

CREATE TABLE devices (
    id bigserial NOT NULL,
    device_id text NOT NULL,
    user_email text NOT NULL,
    gcm_registration_id text,
    device_model text NOT NULL,
    device_manufacturer text NOT NULL,
    device_build text NOT NULL,
    device_version text NOT NULL,
    online_since timestamp with time zone
);

CREATE TABLE empire_alts (
    empire_id bigint NOT NULL,
    alt_blob bytea NOT NULL
);

CREATE TABLE empire_cash_audit (
    id bigserial NOT NULL,
    empire_id bigint NOT NULL,
    cash_before double precision NOT NULL,
    cash_after double precision NOT NULL,
    "time" timestamp with time zone NOT NULL,
    reason bytea NOT NULL
);

CREATE TABLE empire_logins (
    id bigserial NOT NULL,
    empire_id bigint NOT NULL,
    date timestamp with time zone,
    device_model text NOT NULL,
    device_manufacturer text NOT NULL,
    device_build text NOT NULL,
    device_version text NOT NULL
);

CREATE TABLE empire_presences (
    id bigserial NOT NULL,
    empire_id bigint NOT NULL,
    star_id bigint NOT NULL,
    total_goods double precision NOT NULL,
    total_minerals double precision NOT NULL,
    tax_per_hour double precision,
    goods_zero_time timestamp with time zone
);

CREATE TABLE empire_rank_histories (
    id bigserial NOT NULL,
    empire_id bigint NOT NULL,
    date timestamp with time zone,
    rank bigint NOT NULL,
    total_stars bigint NOT NULL,
    total_colonies bigint NOT NULL,
    total_buildings bigint NOT NULL,
    total_ships bigint NOT NULL,
    total_population bigint NOT NULL
);

CREATE TABLE empire_ranks (
    empire_id bigint NOT NULL,
    rank bigint NOT NULL,
    total_stars bigint NOT NULL,
    total_colonies bigint NOT NULL,
    total_buildings bigint NOT NULL,
    total_ships bigint NOT NULL,
    total_population bigint
);

CREATE TABLE empire_shields (
    id bigserial NOT NULL,
    empire_id bigint NOT NULL,
    create_date timestamp with time zone,
    rejected smallint NOT NULL,
    image bytea
);

CREATE TABLE empires (
    id bigserial NOT NULL,
    name text NOT NULL,
    cash double precision NOT NULL,
    home_star_id bigint,
    alliance_id bigint,
    alliance_rank bigint,
    reset_reason text,
    user_email text NOT NULL,
    state bigint DEFAULT 1::bigint NOT NULL,
    remove_ads bigint,
    last_sitrep_read_time timestamp with time zone,
    signup_date timestamp with time zone
);

CREATE TABLE error_reports (
    id bigserial NOT NULL,
    report_date timestamp with time zone,
    empire_id bigint,
    message text,
    exception_class text,
    context text,
    data bytea
);

CREATE TABLE fleet_upgrades (
    id bigserial NOT NULL,
    star_id bigint NOT NULL,
    fleet_id bigint NOT NULL,
    upgrade_id text NOT NULL,
    extra text
);

CREATE TABLE fleets (
    id bigserial NOT NULL,
    star_id bigint NOT NULL,
    sector_id bigint NOT NULL,
    design_id text NOT NULL,
    empire_id bigint,
    num_ships double precision NOT NULL,
    stance bigint NOT NULL,
    state bigint NOT NULL,
    state_start_time timestamp with time zone,
    eta timestamp with time zone,
    target_star_id bigint,
    target_fleet_id bigint,
    time_destroyed timestamp with time zone,
    notes text
);

CREATE TABLE motd (
    motd text NOT NULL
);

CREATE TABLE purchases (
    id bigserial NOT NULL,
    empire_id bigint NOT NULL,
    sku text NOT NULL,
    token text NOT NULL,
    order_id text NOT NULL,
    price text NOT NULL,
    developer_payload text NOT NULL,
    "time" timestamp with time zone,
    sku_extra bytea,
    final_status bigint
);

CREATE TABLE scout_reports (
    id bigserial NOT NULL,
    star_id bigint NOT NULL,
    empire_id bigint NOT NULL,
    date timestamp with time zone,
    report bytea
);

CREATE TABLE sectors (
    id bigserial NOT NULL,
    x bigint NOT NULL,
    y bigint NOT NULL,
    distance_to_centre double precision NOT NULL,
    num_colonies bigint NOT NULL
);

CREATE TABLE sessions (
    id bigserial NOT NULL,
    session_cookie text NOT NULL,
    user_email text NOT NULL,
    login_time timestamp with time zone,
    empire_id bigint,
    alliance_id bigint,
    is_admin smallint NOT NULL,
    inline_notifications bigint
);

CREATE TABLE situation_reports (
    id bigserial NOT NULL,
    empire_id bigint NOT NULL,
    star_id bigint NOT NULL,
    report_time timestamp with time zone,
    report bytea NOT NULL,
    event_kinds bigint
);

CREATE TABLE star_renames (
    id bigserial NOT NULL,
    star_id bigint NOT NULL,
    old_name text NOT NULL,
    new_name text NOT NULL,
    purchase_developer_payload text,
    purchase_order_id text,
    purchase_price text,
    purchase_time timestamp with time zone
);

CREATE TABLE stars (
    id bigserial NOT NULL,
    sector_id bigint NOT NULL,
    x bigint NOT NULL,
    y bigint NOT NULL,
    size bigint NOT NULL,
    empire_count bigint DEFAULT 0::bigint NOT NULL,
    name text NOT NULL,
    star_type bigint NOT NULL,
    planets bytea NOT NULL,
    extra bytea,
    last_simulation timestamp with time zone,
    time_emptied timestamp with time zone
);

ALTER TABLE ONLY abandoned_stars
    ADD CONSTRAINT abandoned_stars_pkey PRIMARY KEY (id);

ALTER TABLE ONLY alliance_bank_balance_audit
    ADD CONSTRAINT alliance_bank_balance_audit_pkey PRIMARY KEY (id);

ALTER TABLE ONLY alliance_join_requests
    ADD CONSTRAINT alliance_join_requests_pkey PRIMARY KEY (id);

ALTER TABLE ONLY alliance_request_votes
    ADD CONSTRAINT alliance_request_votes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY alliance_requests
    ADD CONSTRAINT alliance_requests_pkey PRIMARY KEY (id);

ALTER TABLE ONLY alliance_shields
    ADD CONSTRAINT alliance_shields_pkey PRIMARY KEY (id);

ALTER TABLE ONLY alliances
    ADD CONSTRAINT alliances_pkey PRIMARY KEY (id);

ALTER TABLE ONLY build_requests
    ADD CONSTRAINT build_requests_pkey PRIMARY KEY (id);

ALTER TABLE ONLY buildings
    ADD CONSTRAINT buildings_pkey PRIMARY KEY (id);

ALTER TABLE ONLY chat_abuse_reports
    ADD CONSTRAINT chat_abuse_reports_pkey PRIMARY KEY (id);

ALTER TABLE ONLY chat_conversation_participants
    ADD CONSTRAINT chat_conversation_participants_pkey PRIMARY KEY (id);

ALTER TABLE ONLY chat_conversations
    ADD CONSTRAINT chat_conversations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY chat_messages
    ADD CONSTRAINT chat_messages_pkey PRIMARY KEY (id);

ALTER TABLE ONLY chat_sinbin
    ADD CONSTRAINT chat_sinbin_pkey PRIMARY KEY (id);

ALTER TABLE ONLY colonies
    ADD CONSTRAINT colonies_pkey PRIMARY KEY (id);

ALTER TABLE ONLY combat_reports
    ADD CONSTRAINT combat_reports_pkey PRIMARY KEY (id);

ALTER TABLE ONLY dashboard_stats
    ADD CONSTRAINT dashboard_stats_pkey PRIMARY KEY (date);

ALTER TABLE ONLY devices
    ADD CONSTRAINT devices_pkey PRIMARY KEY (id);

ALTER TABLE ONLY empire_alts
    ADD CONSTRAINT empire_alts_pkey PRIMARY KEY (empire_id);

ALTER TABLE ONLY empire_cash_audit
    ADD CONSTRAINT empire_cash_audit_pkey PRIMARY KEY (id);

ALTER TABLE ONLY empire_logins
    ADD CONSTRAINT empire_logins_pkey PRIMARY KEY (id);

ALTER TABLE ONLY empire_presences
    ADD CONSTRAINT empire_presences_pkey PRIMARY KEY (id);

ALTER TABLE ONLY empire_rank_histories
    ADD CONSTRAINT empire_rank_histories_pkey PRIMARY KEY (id);

ALTER TABLE ONLY empire_ranks
    ADD CONSTRAINT empire_ranks_pkey PRIMARY KEY (empire_id);

ALTER TABLE ONLY empire_shields
    ADD CONSTRAINT empire_shields_pkey PRIMARY KEY (id);

ALTER TABLE ONLY empires
    ADD CONSTRAINT empires_pkey PRIMARY KEY (id);

ALTER TABLE ONLY error_reports
    ADD CONSTRAINT error_reports_pkey PRIMARY KEY (id);

ALTER TABLE ONLY fleet_upgrades
    ADD CONSTRAINT fleet_upgrades_pkey PRIMARY KEY (id);

ALTER TABLE ONLY fleets
    ADD CONSTRAINT fleets_pkey PRIMARY KEY (id);

ALTER TABLE ONLY purchases
    ADD CONSTRAINT purchases_pkey PRIMARY KEY (id);

ALTER TABLE ONLY scout_reports
    ADD CONSTRAINT scout_reports_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sectors
    ADD CONSTRAINT sectors_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sessions
    ADD CONSTRAINT sessions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY situation_reports
    ADD CONSTRAINT situation_reports_pkey PRIMARY KEY (id);

ALTER TABLE ONLY star_renames
    ADD CONSTRAINT star_renames_pkey PRIMARY KEY (id);

ALTER TABLE ONLY stars
    ADD CONSTRAINT stars_pkey PRIMARY KEY (id);

CREATE INDEX idx_27547_fk_abandoned_stars_empires ON abandoned_stars USING btree (empire_id);

CREATE INDEX idx_27547_fk_abandoned_stars_stars ON abandoned_stars USING btree (star_id);

CREATE INDEX idx_27562_fk_alliance_bank_balance_audit_alliance_requests ON alliance_bank_balance_audit USING btree (alliance_request_id);

CREATE INDEX idx_27562_fk_alliance_bank_balance_audit_alliances ON alliance_bank_balance_audit USING btree (alliance_id);

CREATE INDEX idx_27562_fk_alliance_bank_balance_audit_empires ON alliance_bank_balance_audit USING btree (empire_id);

CREATE INDEX idx_27568_fk_alliance_join_requests_alliances ON alliance_join_requests USING btree (alliance_id);

CREATE INDEX idx_27568_fk_alliance_join_requests_empires ON alliance_join_requests USING btree (empire_id);

CREATE INDEX idx_27577_fk_alliance_requests_alliances ON alliance_requests USING btree (alliance_id);

CREATE INDEX idx_27577_fk_alliance_requests_request_empires ON alliance_requests USING btree (request_empire_id);

CREATE INDEX idx_27577_fk_alliance_requests_target_empires ON alliance_requests USING btree (target_empire_id);

CREATE INDEX idx_27586_fk_alliance_request_votes_alliance_requests ON alliance_request_votes USING btree (alliance_request_id);

CREATE INDEX idx_27586_fk_alliance_request_votes_alliances ON alliance_request_votes USING btree (alliance_id);

CREATE INDEX idx_27586_fk_alliance_request_votes_empires ON alliance_request_votes USING btree (empire_id);

CREATE INDEX idx_27592_fk_alliance_shields_alliance ON alliance_shields USING btree (alliance_id);

CREATE INDEX idx_27601_fk_buildings_colonies ON buildings USING btree (colony_id);

CREATE INDEX idx_27601_fk_buildings_empires ON buildings USING btree (empire_id);

CREATE INDEX idx_27601_fk_buildings_stars ON buildings USING btree (star_id);

CREATE INDEX idx_27610_fk_build_requests_colonies ON build_requests USING btree (colony_id);

CREATE INDEX idx_27610_fk_build_requests_empires ON build_requests USING btree (empire_id);

CREATE INDEX idx_27610_fk_build_requests_stars ON build_requests USING btree (star_id);

CREATE INDEX idx_27619_fk_chat_abuse_report_chat_message ON chat_abuse_reports USING btree (chat_msg_id);

CREATE INDEX idx_27619_fk_chat_abuse_report_empire ON chat_abuse_reports USING btree (empire_id);

CREATE INDEX idx_27619_fk_chat_abuse_report_reporting_empire ON chat_abuse_reports USING btree (reporting_empire_id);

CREATE INDEX idx_27631_fk_conversation_participant_conversations ON chat_conversation_participants USING btree (conversation_id);

CREATE INDEX idx_27631_fk_conversation_participant_empires ON chat_conversation_participants USING btree (empire_id);

CREATE INDEX idx_27637_fk_chat_conversation ON chat_messages USING btree (conversation_id);

CREATE INDEX idx_27637_fk_chat_messages_alliances ON chat_messages USING btree (alliance_id);

CREATE INDEX idx_27637_fk_chat_messages_empire ON chat_messages USING btree (empire_id);

CREATE INDEX idx_27652_fk_chat_sinbin_empire ON chat_sinbin USING btree (empire_id);

CREATE INDEX idx_27658_fk_colony_empire ON colonies USING btree (empire_id, star_id);

CREATE INDEX idx_27658_fk_colony_sector ON colonies USING btree (sector_id);

CREATE INDEX idx_27658_fk_colony_star ON colonies USING btree (star_id);

CREATE INDEX idx_27664_fk_combat_reports_stars ON combat_reports USING btree (star_id);

CREATE INDEX idx_27664_ix_combat_reports_end_time ON combat_reports USING btree (end_time);

CREATE INDEX idx_27676_ix_devices_device_id ON devices USING btree (device_id);

CREATE INDEX idx_27676_ix_devices_email_device_id ON devices USING btree (user_email, device_id);

CREATE INDEX idx_27685_fk_empires_alliances ON empires USING btree (alliance_id);

CREATE INDEX idx_27685_fk_empires_home_star ON empires USING btree (home_star_id);

CREATE INDEX idx_27685_ix_empires_user_email ON empires USING btree (user_email);

CREATE UNIQUE INDEX idx_27685_uniq_empires_name ON empires USING btree (name);

CREATE INDEX idx_27701_fk_empire_logins_empires ON empire_logins USING btree (empire_id);

CREATE INDEX idx_27701_ix_empire_logins_date_empire_id ON empire_logins USING btree (date, empire_id);

CREATE INDEX idx_27710_fk_empire_presence_empire ON empire_presences USING btree (empire_id);

CREATE INDEX idx_27710_fk_empire_presence_star ON empire_presences USING btree (star_id);

CREATE UNIQUE INDEX idx_27710_uniq_empire_presence_empire_star ON empire_presences USING btree (empire_id, star_id);

CREATE INDEX idx_27719_fk_empire_rank_histories_empire ON empire_rank_histories USING btree (empire_id);

CREATE INDEX idx_27725_fk_empire_shields_empire ON empire_shields USING btree (empire_id);

CREATE INDEX idx_27743_fk_fleets_empire ON fleets USING btree (empire_id, star_id);

CREATE INDEX idx_27743_fk_fleets_sector ON fleets USING btree (sector_id);

CREATE INDEX idx_27743_fk_fleets_star ON fleets USING btree (star_id);

CREATE INDEX idx_27743_fk_fleets_target_fleet ON fleets USING btree (target_fleet_id);

CREATE INDEX idx_27743_fk_fleets_target_star ON fleets USING btree (target_star_id);

CREATE INDEX idx_27752_fk_fleet_upgrade_fleet ON fleet_upgrades USING btree (fleet_id);

CREATE INDEX idx_27752_fk_fleet_upgrade_star ON fleet_upgrades USING btree (star_id);

CREATE INDEX idx_27767_fk_purchases_empire ON purchases USING btree (empire_id);

CREATE INDEX idx_27776_fk_scout_reports_empires ON scout_reports USING btree (empire_id);

CREATE INDEX idx_27776_fk_scout_reports_stars ON scout_reports USING btree (star_id);

CREATE UNIQUE INDEX idx_27785_uniq_sector_xy ON sectors USING btree (x, y);

CREATE INDEX idx_27791_ix_sessions_session_cookie ON sessions USING btree (session_cookie);

CREATE INDEX idx_27800_ix_situation_reports_empire_date ON situation_reports USING btree (empire_id, report_time);

CREATE INDEX idx_27800_ix_situation_reports_star_empire_date ON situation_reports USING btree (star_id, empire_id, report_time);

CREATE INDEX idx_27809_fk_stars_sectors ON stars USING btree (sector_id);

CREATE INDEX idx_27809_ix_stars_last_simulation ON stars USING btree (empire_count, last_simulation, id);

CREATE INDEX idx_27819_fk_star_renames_stars ON star_renames USING btree (star_id);

ALTER TABLE ONLY abandoned_stars
    ADD CONSTRAINT fk_abandoned_stars_empires FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY abandoned_stars
    ADD CONSTRAINT fk_abandoned_stars_stars FOREIGN KEY (star_id) REFERENCES stars(id);

ALTER TABLE ONLY alliance_bank_balance_audit
    ADD CONSTRAINT fk_alliance_bank_balance_audit_alliance_requests FOREIGN KEY (alliance_request_id) REFERENCES alliance_requests(id);

ALTER TABLE ONLY alliance_bank_balance_audit
    ADD CONSTRAINT fk_alliance_bank_balance_audit_alliances FOREIGN KEY (alliance_id) REFERENCES alliances(id);

ALTER TABLE ONLY alliance_bank_balance_audit
    ADD CONSTRAINT fk_alliance_bank_balance_audit_empires FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY alliance_join_requests
    ADD CONSTRAINT fk_alliance_join_requests_alliances FOREIGN KEY (alliance_id) REFERENCES alliances(id);

ALTER TABLE ONLY alliance_join_requests
    ADD CONSTRAINT fk_alliance_join_requests_empires FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY alliance_request_votes
    ADD CONSTRAINT fk_alliance_request_votes_alliance_requests FOREIGN KEY (alliance_request_id) REFERENCES alliance_requests(id);

ALTER TABLE ONLY alliance_request_votes
    ADD CONSTRAINT fk_alliance_request_votes_alliances FOREIGN KEY (alliance_id) REFERENCES alliances(id);

ALTER TABLE ONLY alliance_request_votes
    ADD CONSTRAINT fk_alliance_request_votes_empires FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY alliance_requests
    ADD CONSTRAINT fk_alliance_requests_alliances FOREIGN KEY (alliance_id) REFERENCES alliances(id);

ALTER TABLE ONLY alliance_requests
    ADD CONSTRAINT fk_alliance_requests_request_empires FOREIGN KEY (request_empire_id) REFERENCES empires(id);

ALTER TABLE ONLY alliance_requests
    ADD CONSTRAINT fk_alliance_requests_target_empires FOREIGN KEY (target_empire_id) REFERENCES empires(id);

ALTER TABLE ONLY alliance_shields
    ADD CONSTRAINT fk_alliance_shields_alliance FOREIGN KEY (alliance_id) REFERENCES alliances(id);

ALTER TABLE ONLY build_requests
    ADD CONSTRAINT fk_build_requests_colonies FOREIGN KEY (colony_id) REFERENCES colonies(id);

ALTER TABLE ONLY build_requests
    ADD CONSTRAINT fk_build_requests_empires FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY build_requests
    ADD CONSTRAINT fk_build_requests_stars FOREIGN KEY (star_id) REFERENCES stars(id);

ALTER TABLE ONLY buildings
    ADD CONSTRAINT fk_buildings_colonies FOREIGN KEY (colony_id) REFERENCES colonies(id) ON DELETE CASCADE;

ALTER TABLE ONLY buildings
    ADD CONSTRAINT fk_buildings_empires FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY buildings
    ADD CONSTRAINT fk_buildings_stars FOREIGN KEY (star_id) REFERENCES stars(id);

ALTER TABLE ONLY chat_abuse_reports
    ADD CONSTRAINT fk_chat_abuse_report_chat_message FOREIGN KEY (chat_msg_id) REFERENCES chat_messages(id);

ALTER TABLE ONLY chat_abuse_reports
    ADD CONSTRAINT fk_chat_abuse_report_empire FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY chat_abuse_reports
    ADD CONSTRAINT fk_chat_abuse_report_reporting_empire FOREIGN KEY (reporting_empire_id) REFERENCES empires(id);

ALTER TABLE ONLY chat_messages
    ADD CONSTRAINT fk_chat_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id);

ALTER TABLE ONLY chat_messages
    ADD CONSTRAINT fk_chat_messages_alliances FOREIGN KEY (alliance_id) REFERENCES alliances(id);

ALTER TABLE ONLY chat_messages
    ADD CONSTRAINT fk_chat_messages_empire FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY chat_sinbin
    ADD CONSTRAINT fk_chat_sinbin_empire FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY colonies
    ADD CONSTRAINT fk_colony_empire FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY colonies
    ADD CONSTRAINT fk_colony_sector FOREIGN KEY (sector_id) REFERENCES sectors(id);

ALTER TABLE ONLY colonies
    ADD CONSTRAINT fk_colony_star FOREIGN KEY (star_id) REFERENCES stars(id);

ALTER TABLE ONLY combat_reports
    ADD CONSTRAINT fk_combat_reports_stars FOREIGN KEY (star_id) REFERENCES stars(id);

ALTER TABLE ONLY chat_conversation_participants
    ADD CONSTRAINT fk_conversation_participant_conversations FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id);

ALTER TABLE ONLY chat_conversation_participants
    ADD CONSTRAINT fk_conversation_participant_empires FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY empire_cash_audit
    ADD CONSTRAINT fk_empire_cash_audit_empire FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY empire_rank_histories
    ADD CONSTRAINT fk_empire_rank_histories_empire FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY empire_shields
    ADD CONSTRAINT fk_empire_shields_empire FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY empires
    ADD CONSTRAINT fk_empires_alliances FOREIGN KEY (alliance_id) REFERENCES alliances(id);

ALTER TABLE ONLY empires
    ADD CONSTRAINT fk_empires_home_star FOREIGN KEY (home_star_id) REFERENCES stars(id);

ALTER TABLE ONLY fleet_upgrades
    ADD CONSTRAINT fk_fleet_upgrade_fleet FOREIGN KEY (fleet_id) REFERENCES fleets(id);

ALTER TABLE ONLY fleet_upgrades
    ADD CONSTRAINT fk_fleet_upgrade_star FOREIGN KEY (star_id) REFERENCES stars(id);

ALTER TABLE ONLY fleets
    ADD CONSTRAINT fk_fleets_empire FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY fleets
    ADD CONSTRAINT fk_fleets_sector FOREIGN KEY (sector_id) REFERENCES sectors(id);

ALTER TABLE ONLY fleets
    ADD CONSTRAINT fk_fleets_star FOREIGN KEY (star_id) REFERENCES stars(id);

ALTER TABLE ONLY fleets
    ADD CONSTRAINT fk_fleets_target_fleet FOREIGN KEY (target_fleet_id) REFERENCES fleets(id);

ALTER TABLE ONLY fleets
    ADD CONSTRAINT fk_fleets_target_star FOREIGN KEY (target_star_id) REFERENCES stars(id);

ALTER TABLE ONLY purchases
    ADD CONSTRAINT fk_purchases_empire FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY scout_reports
    ADD CONSTRAINT fk_scout_reports_empires FOREIGN KEY (empire_id) REFERENCES empires(id);

ALTER TABLE ONLY scout_reports
    ADD CONSTRAINT fk_scout_reports_stars FOREIGN KEY (star_id) REFERENCES stars(id);

ALTER TABLE ONLY star_renames
    ADD CONSTRAINT fk_star_renames_stars FOREIGN KEY (star_id) REFERENCES stars(id);

ALTER TABLE ONLY stars
    ADD CONSTRAINT fk_stars_sectors FOREIGN KEY (sector_id) REFERENCES sectors(id);

