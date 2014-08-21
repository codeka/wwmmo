--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: beta; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA beta;


ALTER SCHEMA beta OWNER TO postgres;

SET search_path = beta, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: abandoned_stars; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE abandoned_stars (
    id bigint NOT NULL,
    star_id bigint NOT NULL,
    empire_id bigint NOT NULL,
    distance_to_centre double precision NOT NULL,
    distance_to_non_abandoned_empire double precision NOT NULL
);


ALTER TABLE beta.abandoned_stars OWNER TO wwmmo_user;

--
-- Name: abandoned_stars_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE abandoned_stars_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.abandoned_stars_id_seq OWNER TO wwmmo_user;

--
-- Name: abandoned_stars_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE abandoned_stars_id_seq OWNED BY abandoned_stars.id;


--
-- Name: alliance_bank_balance_audit; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE alliance_bank_balance_audit (
    id bigint NOT NULL,
    alliance_id bigint NOT NULL,
    alliance_request_id bigint NOT NULL,
    empire_id bigint NOT NULL,
    date timestamp with time zone,
    amount_before double precision NOT NULL,
    amount_after double precision NOT NULL
);


ALTER TABLE beta.alliance_bank_balance_audit OWNER TO wwmmo_user;

--
-- Name: alliance_bank_balance_audit_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE alliance_bank_balance_audit_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.alliance_bank_balance_audit_id_seq OWNER TO wwmmo_user;

--
-- Name: alliance_bank_balance_audit_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE alliance_bank_balance_audit_id_seq OWNED BY alliance_bank_balance_audit.id;


--
-- Name: alliance_join_requests; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE alliance_join_requests (
    id bigint NOT NULL,
    alliance_id bigint NOT NULL,
    empire_id bigint NOT NULL,
    request_date timestamp with time zone,
    message text,
    state bigint NOT NULL
);


ALTER TABLE beta.alliance_join_requests OWNER TO wwmmo_user;

--
-- Name: alliance_join_requests_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE alliance_join_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.alliance_join_requests_id_seq OWNER TO wwmmo_user;

--
-- Name: alliance_join_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE alliance_join_requests_id_seq OWNED BY alliance_join_requests.id;


--
-- Name: alliance_request_votes; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE alliance_request_votes (
    id bigint NOT NULL,
    alliance_id bigint NOT NULL,
    alliance_request_id bigint NOT NULL,
    empire_id bigint NOT NULL,
    votes bigint NOT NULL,
    date timestamp with time zone
);


ALTER TABLE beta.alliance_request_votes OWNER TO wwmmo_user;

--
-- Name: alliance_request_votes_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE alliance_request_votes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.alliance_request_votes_id_seq OWNER TO wwmmo_user;

--
-- Name: alliance_request_votes_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE alliance_request_votes_id_seq OWNED BY alliance_request_votes.id;


--
-- Name: alliance_requests; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE alliance_requests (
    id bigint NOT NULL,
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


ALTER TABLE beta.alliance_requests OWNER TO wwmmo_user;

--
-- Name: alliance_requests_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE alliance_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.alliance_requests_id_seq OWNER TO wwmmo_user;

--
-- Name: alliance_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE alliance_requests_id_seq OWNED BY alliance_requests.id;


--
-- Name: alliance_shields; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE alliance_shields (
    id bigint NOT NULL,
    alliance_id bigint NOT NULL,
    create_date timestamp with time zone,
    image bytea
);


ALTER TABLE beta.alliance_shields OWNER TO wwmmo_user;

--
-- Name: alliance_shields_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE alliance_shields_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.alliance_shields_id_seq OWNER TO wwmmo_user;

--
-- Name: alliance_shields_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE alliance_shields_id_seq OWNED BY alliance_shields.id;


--
-- Name: alliances; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE alliances (
    id bigint NOT NULL,
    name text NOT NULL,
    creator_empire_id bigint NOT NULL,
    created_date timestamp with time zone,
    bank_balance double precision NOT NULL,
    image_updated_date timestamp with time zone
);


ALTER TABLE beta.alliances OWNER TO wwmmo_user;

--
-- Name: alliances_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE alliances_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.alliances_id_seq OWNER TO wwmmo_user;

--
-- Name: alliances_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE alliances_id_seq OWNED BY alliances.id;


--
-- Name: build_requests; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE build_requests (
    id bigint NOT NULL,
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


ALTER TABLE beta.build_requests OWNER TO wwmmo_user;

--
-- Name: build_requests_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE build_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.build_requests_id_seq OWNER TO wwmmo_user;

--
-- Name: build_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE build_requests_id_seq OWNED BY build_requests.id;


--
-- Name: buildings; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE buildings (
    id bigint NOT NULL,
    colony_id bigint NOT NULL,
    star_id bigint NOT NULL,
    empire_id bigint,
    design_id text NOT NULL,
    build_time timestamp with time zone,
    level bigint NOT NULL,
    notes text
);


ALTER TABLE beta.buildings OWNER TO wwmmo_user;

--
-- Name: buildings_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE buildings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.buildings_id_seq OWNER TO wwmmo_user;

--
-- Name: buildings_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE buildings_id_seq OWNED BY buildings.id;


--
-- Name: chat_abuse_reports; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE chat_abuse_reports (
    id bigint NOT NULL,
    chat_msg_id bigint NOT NULL,
    empire_id bigint NOT NULL,
    reporting_empire_id bigint NOT NULL,
    reported_date timestamp with time zone
);


ALTER TABLE beta.chat_abuse_reports OWNER TO wwmmo_user;

--
-- Name: chat_abuse_reports_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE chat_abuse_reports_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.chat_abuse_reports_id_seq OWNER TO wwmmo_user;

--
-- Name: chat_abuse_reports_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE chat_abuse_reports_id_seq OWNED BY chat_abuse_reports.id;


--
-- Name: chat_conversation_participants; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE chat_conversation_participants (
    id bigint NOT NULL,
    conversation_id bigint NOT NULL,
    empire_id bigint NOT NULL,
    is_muted smallint NOT NULL
);


ALTER TABLE beta.chat_conversation_participants OWNER TO wwmmo_user;

--
-- Name: chat_conversation_participants_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE chat_conversation_participants_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.chat_conversation_participants_id_seq OWNER TO wwmmo_user;

--
-- Name: chat_conversation_participants_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE chat_conversation_participants_id_seq OWNED BY chat_conversation_participants.id;


--
-- Name: chat_conversations; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE chat_conversations (
    id bigint NOT NULL
);


ALTER TABLE beta.chat_conversations OWNER TO wwmmo_user;

--
-- Name: chat_conversations_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE chat_conversations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.chat_conversations_id_seq OWNER TO wwmmo_user;

--
-- Name: chat_conversations_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE chat_conversations_id_seq OWNED BY chat_conversations.id;


--
-- Name: chat_messages; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE chat_messages (
    id bigint NOT NULL,
    empire_id bigint,
    alliance_id bigint,
    message text NOT NULL,
    message_en text,
    profanity_level bigint,
    posted_date timestamp with time zone,
    conversation_id bigint,
    action bigint
);


ALTER TABLE beta.chat_messages OWNER TO wwmmo_user;

--
-- Name: chat_messages_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE chat_messages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.chat_messages_id_seq OWNER TO wwmmo_user;

--
-- Name: chat_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE chat_messages_id_seq OWNED BY chat_messages.id;


--
-- Name: chat_profane_words; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE chat_profane_words (
    profanity_level bigint NOT NULL,
    words text NOT NULL
);


ALTER TABLE beta.chat_profane_words OWNER TO wwmmo_user;

--
-- Name: chat_sinbin; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE chat_sinbin (
    id bigint NOT NULL,
    empire_id bigint NOT NULL,
    expiry timestamp with time zone
);


ALTER TABLE beta.chat_sinbin OWNER TO wwmmo_user;

--
-- Name: chat_sinbin_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE chat_sinbin_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.chat_sinbin_id_seq OWNER TO wwmmo_user;

--
-- Name: chat_sinbin_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE chat_sinbin_id_seq OWNED BY chat_sinbin.id;


--
-- Name: colonies; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE colonies (
    id bigint NOT NULL,
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


ALTER TABLE beta.colonies OWNER TO wwmmo_user;

--
-- Name: colonies_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE colonies_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.colonies_id_seq OWNER TO wwmmo_user;

--
-- Name: colonies_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE colonies_id_seq OWNED BY colonies.id;


--
-- Name: combat_reports; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE combat_reports (
    id bigint NOT NULL,
    star_id bigint NOT NULL,
    start_time timestamp with time zone,
    end_time timestamp with time zone,
    rounds bytea NOT NULL
);


ALTER TABLE beta.combat_reports OWNER TO wwmmo_user;

--
-- Name: combat_reports_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE combat_reports_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.combat_reports_id_seq OWNER TO wwmmo_user;

--
-- Name: combat_reports_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE combat_reports_id_seq OWNED BY combat_reports.id;


--
-- Name: dashboard_stats; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE dashboard_stats (
    date date NOT NULL,
    active_1d bigint NOT NULL,
    active_7d bigint NOT NULL,
    new_signups bigint NOT NULL
);


ALTER TABLE beta.dashboard_stats OWNER TO wwmmo_user;

--
-- Name: devices; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE devices (
    id bigint NOT NULL,
    device_id text NOT NULL,
    user_email text NOT NULL,
    gcm_registration_id text,
    device_model text NOT NULL,
    device_manufacturer text NOT NULL,
    device_build text NOT NULL,
    device_version text NOT NULL,
    online_since timestamp with time zone
);


ALTER TABLE beta.devices OWNER TO wwmmo_user;

--
-- Name: devices_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE devices_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.devices_id_seq OWNER TO wwmmo_user;

--
-- Name: devices_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE devices_id_seq OWNED BY devices.id;


--
-- Name: empire_alts; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE empire_alts (
    empire_id bigint NOT NULL,
    alt_blob bytea NOT NULL
);


ALTER TABLE beta.empire_alts OWNER TO wwmmo_user;

--
-- Name: empire_cash_audit; Type: TABLE; Schema: beta; Owner: postgres; Tablespace: 
--

CREATE TABLE empire_cash_audit (
    id bigint NOT NULL,
    empire_id bigint NOT NULL,
    cash_before double precision NOT NULL,
    cash_after double precision NOT NULL,
    "time" timestamp with time zone NOT NULL,
    reason bytea NOT NULL
);


ALTER TABLE beta.empire_cash_audit OWNER TO postgres;

--
-- Name: empire_cash_audit_id_seq; Type: SEQUENCE; Schema: beta; Owner: postgres
--

CREATE SEQUENCE empire_cash_audit_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.empire_cash_audit_id_seq OWNER TO postgres;

--
-- Name: empire_cash_audit_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: postgres
--

ALTER SEQUENCE empire_cash_audit_id_seq OWNED BY empire_cash_audit.id;


--
-- Name: empire_logins; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE empire_logins (
    id bigint NOT NULL,
    empire_id bigint NOT NULL,
    date timestamp with time zone,
    device_model text NOT NULL,
    device_manufacturer text NOT NULL,
    device_build text NOT NULL,
    device_version text NOT NULL
);


ALTER TABLE beta.empire_logins OWNER TO wwmmo_user;

--
-- Name: empire_logins_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE empire_logins_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.empire_logins_id_seq OWNER TO wwmmo_user;

--
-- Name: empire_logins_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE empire_logins_id_seq OWNED BY empire_logins.id;


--
-- Name: empire_presences; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE empire_presences (
    id bigint NOT NULL,
    empire_id bigint NOT NULL,
    star_id bigint NOT NULL,
    total_goods double precision NOT NULL,
    total_minerals double precision NOT NULL,
    tax_per_hour double precision,
    goods_zero_time timestamp with time zone
);


ALTER TABLE beta.empire_presences OWNER TO wwmmo_user;

--
-- Name: empire_presences_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE empire_presences_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.empire_presences_id_seq OWNER TO wwmmo_user;

--
-- Name: empire_presences_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE empire_presences_id_seq OWNED BY empire_presences.id;


--
-- Name: empire_rank_histories; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE empire_rank_histories (
    id bigint NOT NULL,
    empire_id bigint NOT NULL,
    date timestamp with time zone,
    rank bigint NOT NULL,
    total_stars bigint NOT NULL,
    total_colonies bigint NOT NULL,
    total_buildings bigint NOT NULL,
    total_ships bigint NOT NULL,
    total_population bigint NOT NULL
);


ALTER TABLE beta.empire_rank_histories OWNER TO wwmmo_user;

--
-- Name: empire_rank_histories_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE empire_rank_histories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.empire_rank_histories_id_seq OWNER TO wwmmo_user;

--
-- Name: empire_rank_histories_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE empire_rank_histories_id_seq OWNED BY empire_rank_histories.id;


--
-- Name: empire_ranks; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE empire_ranks (
    empire_id bigint NOT NULL,
    rank bigint NOT NULL,
    total_stars bigint NOT NULL,
    total_colonies bigint NOT NULL,
    total_buildings bigint NOT NULL,
    total_ships bigint NOT NULL,
    total_population bigint
);


ALTER TABLE beta.empire_ranks OWNER TO wwmmo_user;

--
-- Name: empire_shields; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE empire_shields (
    id bigint NOT NULL,
    empire_id bigint NOT NULL,
    create_date timestamp with time zone,
    rejected smallint NOT NULL,
    image bytea
);


ALTER TABLE beta.empire_shields OWNER TO wwmmo_user;

--
-- Name: empire_shields_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE empire_shields_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.empire_shields_id_seq OWNER TO wwmmo_user;

--
-- Name: empire_shields_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE empire_shields_id_seq OWNED BY empire_shields.id;


--
-- Name: empires; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE empires (
    id bigint NOT NULL,
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


ALTER TABLE beta.empires OWNER TO wwmmo_user;

--
-- Name: empires_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE empires_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.empires_id_seq OWNER TO wwmmo_user;

--
-- Name: empires_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE empires_id_seq OWNED BY empires.id;


--
-- Name: error_reports; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE error_reports (
    id bigint NOT NULL,
    report_date timestamp with time zone,
    empire_id bigint,
    message text,
    exception_class text,
    context text,
    data bytea
);


ALTER TABLE beta.error_reports OWNER TO wwmmo_user;

--
-- Name: error_reports_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE error_reports_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.error_reports_id_seq OWNER TO wwmmo_user;

--
-- Name: error_reports_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE error_reports_id_seq OWNED BY error_reports.id;


--
-- Name: fleet_upgrades; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE fleet_upgrades (
    id bigint NOT NULL,
    star_id bigint NOT NULL,
    fleet_id bigint NOT NULL,
    upgrade_id text NOT NULL,
    extra text
);


ALTER TABLE beta.fleet_upgrades OWNER TO wwmmo_user;

--
-- Name: fleet_upgrades_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE fleet_upgrades_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.fleet_upgrades_id_seq OWNER TO wwmmo_user;

--
-- Name: fleet_upgrades_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE fleet_upgrades_id_seq OWNED BY fleet_upgrades.id;


--
-- Name: fleets; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE fleets (
    id bigint NOT NULL,
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


ALTER TABLE beta.fleets OWNER TO wwmmo_user;

--
-- Name: fleets_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE fleets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.fleets_id_seq OWNER TO wwmmo_user;

--
-- Name: fleets_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE fleets_id_seq OWNED BY fleets.id;


--
-- Name: motd; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE motd (
    motd text NOT NULL
);


ALTER TABLE beta.motd OWNER TO wwmmo_user;

--
-- Name: purchases; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE purchases (
    id bigint NOT NULL,
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


ALTER TABLE beta.purchases OWNER TO wwmmo_user;

--
-- Name: purchases_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE purchases_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.purchases_id_seq OWNER TO wwmmo_user;

--
-- Name: purchases_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE purchases_id_seq OWNED BY purchases.id;


--
-- Name: scout_reports; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE scout_reports (
    id bigint NOT NULL,
    star_id bigint NOT NULL,
    empire_id bigint NOT NULL,
    date timestamp with time zone,
    report bytea
);


ALTER TABLE beta.scout_reports OWNER TO wwmmo_user;

--
-- Name: scout_reports_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE scout_reports_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.scout_reports_id_seq OWNER TO wwmmo_user;

--
-- Name: scout_reports_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE scout_reports_id_seq OWNED BY scout_reports.id;


--
-- Name: sectors; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE sectors (
    id bigint NOT NULL,
    x bigint NOT NULL,
    y bigint NOT NULL,
    distance_to_centre double precision NOT NULL,
    num_colonies bigint NOT NULL
);


ALTER TABLE beta.sectors OWNER TO wwmmo_user;

--
-- Name: sectors_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE sectors_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.sectors_id_seq OWNER TO wwmmo_user;

--
-- Name: sectors_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE sectors_id_seq OWNED BY sectors.id;


--
-- Name: sessions; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE sessions (
    id bigint NOT NULL,
    session_cookie text NOT NULL,
    user_email text NOT NULL,
    login_time timestamp with time zone,
    empire_id bigint,
    alliance_id bigint,
    is_admin smallint NOT NULL,
    inline_notifications bigint
);


ALTER TABLE beta.sessions OWNER TO wwmmo_user;

--
-- Name: sessions_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE sessions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.sessions_id_seq OWNER TO wwmmo_user;

--
-- Name: sessions_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE sessions_id_seq OWNED BY sessions.id;


--
-- Name: situation_reports; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE situation_reports (
    id bigint NOT NULL,
    empire_id bigint NOT NULL,
    star_id bigint NOT NULL,
    report_time timestamp with time zone,
    report bytea NOT NULL,
    event_kinds bigint
);


ALTER TABLE beta.situation_reports OWNER TO wwmmo_user;

--
-- Name: situation_reports_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE situation_reports_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.situation_reports_id_seq OWNER TO wwmmo_user;

--
-- Name: situation_reports_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE situation_reports_id_seq OWNED BY situation_reports.id;


--
-- Name: star_renames; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE star_renames (
    id bigint NOT NULL,
    star_id bigint NOT NULL,
    old_name text NOT NULL,
    new_name text NOT NULL,
    purchase_developer_payload text,
    purchase_order_id text,
    purchase_price text,
    purchase_time timestamp with time zone
);


ALTER TABLE beta.star_renames OWNER TO wwmmo_user;

--
-- Name: star_renames_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE star_renames_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.star_renames_id_seq OWNER TO wwmmo_user;

--
-- Name: star_renames_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE star_renames_id_seq OWNED BY star_renames.id;


--
-- Name: stars; Type: TABLE; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE TABLE stars (
    id bigint NOT NULL,
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


ALTER TABLE beta.stars OWNER TO wwmmo_user;

--
-- Name: stars_id_seq; Type: SEQUENCE; Schema: beta; Owner: wwmmo_user
--

CREATE SEQUENCE stars_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE beta.stars_id_seq OWNER TO wwmmo_user;

--
-- Name: stars_id_seq; Type: SEQUENCE OWNED BY; Schema: beta; Owner: wwmmo_user
--

ALTER SEQUENCE stars_id_seq OWNED BY stars.id;


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY abandoned_stars ALTER COLUMN id SET DEFAULT nextval('abandoned_stars_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_bank_balance_audit ALTER COLUMN id SET DEFAULT nextval('alliance_bank_balance_audit_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_join_requests ALTER COLUMN id SET DEFAULT nextval('alliance_join_requests_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_request_votes ALTER COLUMN id SET DEFAULT nextval('alliance_request_votes_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_requests ALTER COLUMN id SET DEFAULT nextval('alliance_requests_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_shields ALTER COLUMN id SET DEFAULT nextval('alliance_shields_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliances ALTER COLUMN id SET DEFAULT nextval('alliances_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY build_requests ALTER COLUMN id SET DEFAULT nextval('build_requests_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY buildings ALTER COLUMN id SET DEFAULT nextval('buildings_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY chat_abuse_reports ALTER COLUMN id SET DEFAULT nextval('chat_abuse_reports_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY chat_conversation_participants ALTER COLUMN id SET DEFAULT nextval('chat_conversation_participants_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY chat_conversations ALTER COLUMN id SET DEFAULT nextval('chat_conversations_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY chat_messages ALTER COLUMN id SET DEFAULT nextval('chat_messages_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY chat_sinbin ALTER COLUMN id SET DEFAULT nextval('chat_sinbin_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY colonies ALTER COLUMN id SET DEFAULT nextval('colonies_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY combat_reports ALTER COLUMN id SET DEFAULT nextval('combat_reports_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY devices ALTER COLUMN id SET DEFAULT nextval('devices_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: postgres
--

ALTER TABLE ONLY empire_cash_audit ALTER COLUMN id SET DEFAULT nextval('empire_cash_audit_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY empire_logins ALTER COLUMN id SET DEFAULT nextval('empire_logins_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY empire_presences ALTER COLUMN id SET DEFAULT nextval('empire_presences_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY empire_rank_histories ALTER COLUMN id SET DEFAULT nextval('empire_rank_histories_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY empire_shields ALTER COLUMN id SET DEFAULT nextval('empire_shields_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY empires ALTER COLUMN id SET DEFAULT nextval('empires_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY error_reports ALTER COLUMN id SET DEFAULT nextval('error_reports_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY fleet_upgrades ALTER COLUMN id SET DEFAULT nextval('fleet_upgrades_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY fleets ALTER COLUMN id SET DEFAULT nextval('fleets_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY purchases ALTER COLUMN id SET DEFAULT nextval('purchases_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY scout_reports ALTER COLUMN id SET DEFAULT nextval('scout_reports_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY sectors ALTER COLUMN id SET DEFAULT nextval('sectors_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY sessions ALTER COLUMN id SET DEFAULT nextval('sessions_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY situation_reports ALTER COLUMN id SET DEFAULT nextval('situation_reports_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY star_renames ALTER COLUMN id SET DEFAULT nextval('star_renames_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY stars ALTER COLUMN id SET DEFAULT nextval('stars_id_seq'::regclass);


--
-- Name: abandoned_stars_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY abandoned_stars
    ADD CONSTRAINT abandoned_stars_pkey PRIMARY KEY (id);


--
-- Name: alliance_bank_balance_audit_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY alliance_bank_balance_audit
    ADD CONSTRAINT alliance_bank_balance_audit_pkey PRIMARY KEY (id);


--
-- Name: alliance_join_requests_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY alliance_join_requests
    ADD CONSTRAINT alliance_join_requests_pkey PRIMARY KEY (id);


--
-- Name: alliance_request_votes_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY alliance_request_votes
    ADD CONSTRAINT alliance_request_votes_pkey PRIMARY KEY (id);


--
-- Name: alliance_requests_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY alliance_requests
    ADD CONSTRAINT alliance_requests_pkey PRIMARY KEY (id);


--
-- Name: alliance_shields_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY alliance_shields
    ADD CONSTRAINT alliance_shields_pkey PRIMARY KEY (id);


--
-- Name: alliances_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY alliances
    ADD CONSTRAINT alliances_pkey PRIMARY KEY (id);


--
-- Name: build_requests_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY build_requests
    ADD CONSTRAINT build_requests_pkey PRIMARY KEY (id);


--
-- Name: buildings_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY buildings
    ADD CONSTRAINT buildings_pkey PRIMARY KEY (id);


--
-- Name: chat_abuse_reports_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY chat_abuse_reports
    ADD CONSTRAINT chat_abuse_reports_pkey PRIMARY KEY (id);


--
-- Name: chat_conversation_participants_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY chat_conversation_participants
    ADD CONSTRAINT chat_conversation_participants_pkey PRIMARY KEY (id);


--
-- Name: chat_conversations_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY chat_conversations
    ADD CONSTRAINT chat_conversations_pkey PRIMARY KEY (id);


--
-- Name: chat_messages_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY chat_messages
    ADD CONSTRAINT chat_messages_pkey PRIMARY KEY (id);


--
-- Name: chat_sinbin_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY chat_sinbin
    ADD CONSTRAINT chat_sinbin_pkey PRIMARY KEY (id);


--
-- Name: colonies_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY colonies
    ADD CONSTRAINT colonies_pkey PRIMARY KEY (id);


--
-- Name: combat_reports_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY combat_reports
    ADD CONSTRAINT combat_reports_pkey PRIMARY KEY (id);


--
-- Name: dashboard_stats_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY dashboard_stats
    ADD CONSTRAINT dashboard_stats_pkey PRIMARY KEY (date);


--
-- Name: devices_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY devices
    ADD CONSTRAINT devices_pkey PRIMARY KEY (id);


--
-- Name: empire_alts_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY empire_alts
    ADD CONSTRAINT empire_alts_pkey PRIMARY KEY (empire_id);


--
-- Name: empire_cash_audit_pkey; Type: CONSTRAINT; Schema: beta; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY empire_cash_audit
    ADD CONSTRAINT empire_cash_audit_pkey PRIMARY KEY (id);


--
-- Name: empire_logins_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY empire_logins
    ADD CONSTRAINT empire_logins_pkey PRIMARY KEY (id);


--
-- Name: empire_presences_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY empire_presences
    ADD CONSTRAINT empire_presences_pkey PRIMARY KEY (id);


--
-- Name: empire_rank_histories_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY empire_rank_histories
    ADD CONSTRAINT empire_rank_histories_pkey PRIMARY KEY (id);


--
-- Name: empire_ranks_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY empire_ranks
    ADD CONSTRAINT empire_ranks_pkey PRIMARY KEY (empire_id);


--
-- Name: empire_shields_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY empire_shields
    ADD CONSTRAINT empire_shields_pkey PRIMARY KEY (id);


--
-- Name: empires_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY empires
    ADD CONSTRAINT empires_pkey PRIMARY KEY (id);


--
-- Name: error_reports_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY error_reports
    ADD CONSTRAINT error_reports_pkey PRIMARY KEY (id);


--
-- Name: fleet_upgrades_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY fleet_upgrades
    ADD CONSTRAINT fleet_upgrades_pkey PRIMARY KEY (id);


--
-- Name: fleets_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY fleets
    ADD CONSTRAINT fleets_pkey PRIMARY KEY (id);


--
-- Name: purchases_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY purchases
    ADD CONSTRAINT purchases_pkey PRIMARY KEY (id);


--
-- Name: scout_reports_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY scout_reports
    ADD CONSTRAINT scout_reports_pkey PRIMARY KEY (id);


--
-- Name: sectors_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY sectors
    ADD CONSTRAINT sectors_pkey PRIMARY KEY (id);


--
-- Name: sessions_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY sessions
    ADD CONSTRAINT sessions_pkey PRIMARY KEY (id);


--
-- Name: situation_reports_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY situation_reports
    ADD CONSTRAINT situation_reports_pkey PRIMARY KEY (id);


--
-- Name: star_renames_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY star_renames
    ADD CONSTRAINT star_renames_pkey PRIMARY KEY (id);


--
-- Name: stars_pkey; Type: CONSTRAINT; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

ALTER TABLE ONLY stars
    ADD CONSTRAINT stars_pkey PRIMARY KEY (id);


--
-- Name: idx_27547_fk_abandoned_stars_empires; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27547_fk_abandoned_stars_empires ON abandoned_stars USING btree (empire_id);


--
-- Name: idx_27547_fk_abandoned_stars_stars; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27547_fk_abandoned_stars_stars ON abandoned_stars USING btree (star_id);


--
-- Name: idx_27562_fk_alliance_bank_balance_audit_alliance_requests; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27562_fk_alliance_bank_balance_audit_alliance_requests ON alliance_bank_balance_audit USING btree (alliance_request_id);


--
-- Name: idx_27562_fk_alliance_bank_balance_audit_alliances; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27562_fk_alliance_bank_balance_audit_alliances ON alliance_bank_balance_audit USING btree (alliance_id);


--
-- Name: idx_27562_fk_alliance_bank_balance_audit_empires; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27562_fk_alliance_bank_balance_audit_empires ON alliance_bank_balance_audit USING btree (empire_id);


--
-- Name: idx_27568_fk_alliance_join_requests_alliances; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27568_fk_alliance_join_requests_alliances ON alliance_join_requests USING btree (alliance_id);


--
-- Name: idx_27568_fk_alliance_join_requests_empires; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27568_fk_alliance_join_requests_empires ON alliance_join_requests USING btree (empire_id);


--
-- Name: idx_27577_fk_alliance_requests_alliances; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27577_fk_alliance_requests_alliances ON alliance_requests USING btree (alliance_id);


--
-- Name: idx_27577_fk_alliance_requests_request_empires; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27577_fk_alliance_requests_request_empires ON alliance_requests USING btree (request_empire_id);


--
-- Name: idx_27577_fk_alliance_requests_target_empires; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27577_fk_alliance_requests_target_empires ON alliance_requests USING btree (target_empire_id);


--
-- Name: idx_27586_fk_alliance_request_votes_alliance_requests; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27586_fk_alliance_request_votes_alliance_requests ON alliance_request_votes USING btree (alliance_request_id);


--
-- Name: idx_27586_fk_alliance_request_votes_alliances; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27586_fk_alliance_request_votes_alliances ON alliance_request_votes USING btree (alliance_id);


--
-- Name: idx_27586_fk_alliance_request_votes_empires; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27586_fk_alliance_request_votes_empires ON alliance_request_votes USING btree (empire_id);


--
-- Name: idx_27592_fk_alliance_shields_alliance; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27592_fk_alliance_shields_alliance ON alliance_shields USING btree (alliance_id);


--
-- Name: idx_27601_fk_buildings_colonies; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27601_fk_buildings_colonies ON buildings USING btree (colony_id);


--
-- Name: idx_27601_fk_buildings_empires; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27601_fk_buildings_empires ON buildings USING btree (empire_id);


--
-- Name: idx_27601_fk_buildings_stars; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27601_fk_buildings_stars ON buildings USING btree (star_id);


--
-- Name: idx_27610_fk_build_requests_colonies; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27610_fk_build_requests_colonies ON build_requests USING btree (colony_id);


--
-- Name: idx_27610_fk_build_requests_empires; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27610_fk_build_requests_empires ON build_requests USING btree (empire_id);


--
-- Name: idx_27610_fk_build_requests_stars; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27610_fk_build_requests_stars ON build_requests USING btree (star_id);


--
-- Name: idx_27619_fk_chat_abuse_report_chat_message; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27619_fk_chat_abuse_report_chat_message ON chat_abuse_reports USING btree (chat_msg_id);


--
-- Name: idx_27619_fk_chat_abuse_report_empire; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27619_fk_chat_abuse_report_empire ON chat_abuse_reports USING btree (empire_id);


--
-- Name: idx_27619_fk_chat_abuse_report_reporting_empire; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27619_fk_chat_abuse_report_reporting_empire ON chat_abuse_reports USING btree (reporting_empire_id);


--
-- Name: idx_27631_fk_conversation_participant_conversations; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27631_fk_conversation_participant_conversations ON chat_conversation_participants USING btree (conversation_id);


--
-- Name: idx_27631_fk_conversation_participant_empires; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27631_fk_conversation_participant_empires ON chat_conversation_participants USING btree (empire_id);


--
-- Name: idx_27637_fk_chat_conversation; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27637_fk_chat_conversation ON chat_messages USING btree (conversation_id);


--
-- Name: idx_27637_fk_chat_messages_alliances; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27637_fk_chat_messages_alliances ON chat_messages USING btree (alliance_id);


--
-- Name: idx_27637_fk_chat_messages_empire; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27637_fk_chat_messages_empire ON chat_messages USING btree (empire_id);


--
-- Name: idx_27652_fk_chat_sinbin_empire; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27652_fk_chat_sinbin_empire ON chat_sinbin USING btree (empire_id);


--
-- Name: idx_27658_fk_colony_empire; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27658_fk_colony_empire ON colonies USING btree (empire_id, star_id);


--
-- Name: idx_27658_fk_colony_sector; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27658_fk_colony_sector ON colonies USING btree (sector_id);


--
-- Name: idx_27658_fk_colony_star; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27658_fk_colony_star ON colonies USING btree (star_id);


--
-- Name: idx_27664_fk_combat_reports_stars; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27664_fk_combat_reports_stars ON combat_reports USING btree (star_id);


--
-- Name: idx_27664_ix_combat_reports_end_time; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27664_ix_combat_reports_end_time ON combat_reports USING btree (end_time);


--
-- Name: idx_27676_ix_devices_device_id; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27676_ix_devices_device_id ON devices USING btree (device_id);


--
-- Name: idx_27676_ix_devices_email_device_id; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27676_ix_devices_email_device_id ON devices USING btree (user_email, device_id);


--
-- Name: idx_27685_fk_empires_alliances; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27685_fk_empires_alliances ON empires USING btree (alliance_id);


--
-- Name: idx_27685_fk_empires_home_star; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27685_fk_empires_home_star ON empires USING btree (home_star_id);


--
-- Name: idx_27685_ix_empires_user_email; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27685_ix_empires_user_email ON empires USING btree (user_email);


--
-- Name: idx_27685_uniq_empires_name; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE UNIQUE INDEX idx_27685_uniq_empires_name ON empires USING btree (name);


--
-- Name: idx_27701_fk_empire_logins_empires; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27701_fk_empire_logins_empires ON empire_logins USING btree (empire_id);


--
-- Name: idx_27701_ix_empire_logins_date_empire_id; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27701_ix_empire_logins_date_empire_id ON empire_logins USING btree (date, empire_id);


--
-- Name: idx_27710_fk_empire_presence_empire; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27710_fk_empire_presence_empire ON empire_presences USING btree (empire_id);


--
-- Name: idx_27710_fk_empire_presence_star; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27710_fk_empire_presence_star ON empire_presences USING btree (star_id);


--
-- Name: idx_27710_uniq_empire_presence_empire_star; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE UNIQUE INDEX idx_27710_uniq_empire_presence_empire_star ON empire_presences USING btree (empire_id, star_id);


--
-- Name: idx_27719_fk_empire_rank_histories_empire; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27719_fk_empire_rank_histories_empire ON empire_rank_histories USING btree (empire_id);


--
-- Name: idx_27725_fk_empire_shields_empire; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27725_fk_empire_shields_empire ON empire_shields USING btree (empire_id);


--
-- Name: idx_27743_fk_fleets_empire; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27743_fk_fleets_empire ON fleets USING btree (empire_id, star_id);


--
-- Name: idx_27743_fk_fleets_sector; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27743_fk_fleets_sector ON fleets USING btree (sector_id);


--
-- Name: idx_27743_fk_fleets_star; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27743_fk_fleets_star ON fleets USING btree (star_id);


--
-- Name: idx_27743_fk_fleets_target_fleet; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27743_fk_fleets_target_fleet ON fleets USING btree (target_fleet_id);


--
-- Name: idx_27743_fk_fleets_target_star; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27743_fk_fleets_target_star ON fleets USING btree (target_star_id);


--
-- Name: idx_27752_fk_fleet_upgrade_fleet; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27752_fk_fleet_upgrade_fleet ON fleet_upgrades USING btree (fleet_id);


--
-- Name: idx_27752_fk_fleet_upgrade_star; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27752_fk_fleet_upgrade_star ON fleet_upgrades USING btree (star_id);


--
-- Name: idx_27767_fk_purchases_empire; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27767_fk_purchases_empire ON purchases USING btree (empire_id);


--
-- Name: idx_27776_fk_scout_reports_empires; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27776_fk_scout_reports_empires ON scout_reports USING btree (empire_id);


--
-- Name: idx_27776_fk_scout_reports_stars; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27776_fk_scout_reports_stars ON scout_reports USING btree (star_id);


--
-- Name: idx_27785_uniq_sector_xy; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE UNIQUE INDEX idx_27785_uniq_sector_xy ON sectors USING btree (x, y);


--
-- Name: idx_27791_ix_sessions_session_cookie; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27791_ix_sessions_session_cookie ON sessions USING btree (session_cookie);


--
-- Name: idx_27800_ix_situation_reports_empire_date; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27800_ix_situation_reports_empire_date ON situation_reports USING btree (empire_id, report_time);


--
-- Name: idx_27800_ix_situation_reports_star_empire_date; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27800_ix_situation_reports_star_empire_date ON situation_reports USING btree (star_id, empire_id, report_time);


--
-- Name: idx_27809_fk_stars_sectors; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27809_fk_stars_sectors ON stars USING btree (sector_id);


--
-- Name: idx_27809_ix_stars_last_simulation; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27809_ix_stars_last_simulation ON stars USING btree (empire_count, last_simulation, id);


--
-- Name: idx_27819_fk_star_renames_stars; Type: INDEX; Schema: beta; Owner: wwmmo_user; Tablespace: 
--

CREATE INDEX idx_27819_fk_star_renames_stars ON star_renames USING btree (star_id);


--
-- Name: fk_abandoned_stars_empires; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY abandoned_stars
    ADD CONSTRAINT fk_abandoned_stars_empires FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_abandoned_stars_stars; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY abandoned_stars
    ADD CONSTRAINT fk_abandoned_stars_stars FOREIGN KEY (star_id) REFERENCES stars(id);


--
-- Name: fk_alliance_bank_balance_audit_alliance_requests; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_bank_balance_audit
    ADD CONSTRAINT fk_alliance_bank_balance_audit_alliance_requests FOREIGN KEY (alliance_request_id) REFERENCES alliance_requests(id);


--
-- Name: fk_alliance_bank_balance_audit_alliances; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_bank_balance_audit
    ADD CONSTRAINT fk_alliance_bank_balance_audit_alliances FOREIGN KEY (alliance_id) REFERENCES alliances(id);


--
-- Name: fk_alliance_bank_balance_audit_empires; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_bank_balance_audit
    ADD CONSTRAINT fk_alliance_bank_balance_audit_empires FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_alliance_join_requests_alliances; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_join_requests
    ADD CONSTRAINT fk_alliance_join_requests_alliances FOREIGN KEY (alliance_id) REFERENCES alliances(id);


--
-- Name: fk_alliance_join_requests_empires; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_join_requests
    ADD CONSTRAINT fk_alliance_join_requests_empires FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_alliance_request_votes_alliance_requests; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_request_votes
    ADD CONSTRAINT fk_alliance_request_votes_alliance_requests FOREIGN KEY (alliance_request_id) REFERENCES alliance_requests(id);


--
-- Name: fk_alliance_request_votes_alliances; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_request_votes
    ADD CONSTRAINT fk_alliance_request_votes_alliances FOREIGN KEY (alliance_id) REFERENCES alliances(id);


--
-- Name: fk_alliance_request_votes_empires; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_request_votes
    ADD CONSTRAINT fk_alliance_request_votes_empires FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_alliance_requests_alliances; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_requests
    ADD CONSTRAINT fk_alliance_requests_alliances FOREIGN KEY (alliance_id) REFERENCES alliances(id);


--
-- Name: fk_alliance_requests_request_empires; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_requests
    ADD CONSTRAINT fk_alliance_requests_request_empires FOREIGN KEY (request_empire_id) REFERENCES empires(id);


--
-- Name: fk_alliance_requests_target_empires; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_requests
    ADD CONSTRAINT fk_alliance_requests_target_empires FOREIGN KEY (target_empire_id) REFERENCES empires(id);


--
-- Name: fk_alliance_shields_alliance; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY alliance_shields
    ADD CONSTRAINT fk_alliance_shields_alliance FOREIGN KEY (alliance_id) REFERENCES alliances(id);


--
-- Name: fk_build_requests_colonies; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY build_requests
    ADD CONSTRAINT fk_build_requests_colonies FOREIGN KEY (colony_id) REFERENCES colonies(id);


--
-- Name: fk_build_requests_empires; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY build_requests
    ADD CONSTRAINT fk_build_requests_empires FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_build_requests_stars; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY build_requests
    ADD CONSTRAINT fk_build_requests_stars FOREIGN KEY (star_id) REFERENCES stars(id);


--
-- Name: fk_buildings_colonies; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY buildings
    ADD CONSTRAINT fk_buildings_colonies FOREIGN KEY (colony_id) REFERENCES colonies(id) ON DELETE CASCADE;


--
-- Name: fk_buildings_empires; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY buildings
    ADD CONSTRAINT fk_buildings_empires FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_buildings_stars; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY buildings
    ADD CONSTRAINT fk_buildings_stars FOREIGN KEY (star_id) REFERENCES stars(id);


--
-- Name: fk_chat_abuse_report_chat_message; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY chat_abuse_reports
    ADD CONSTRAINT fk_chat_abuse_report_chat_message FOREIGN KEY (chat_msg_id) REFERENCES chat_messages(id);


--
-- Name: fk_chat_abuse_report_empire; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY chat_abuse_reports
    ADD CONSTRAINT fk_chat_abuse_report_empire FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_chat_abuse_report_reporting_empire; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY chat_abuse_reports
    ADD CONSTRAINT fk_chat_abuse_report_reporting_empire FOREIGN KEY (reporting_empire_id) REFERENCES empires(id);


--
-- Name: fk_chat_conversation; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY chat_messages
    ADD CONSTRAINT fk_chat_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id);


--
-- Name: fk_chat_messages_alliances; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY chat_messages
    ADD CONSTRAINT fk_chat_messages_alliances FOREIGN KEY (alliance_id) REFERENCES alliances(id);


--
-- Name: fk_chat_messages_empire; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY chat_messages
    ADD CONSTRAINT fk_chat_messages_empire FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_chat_sinbin_empire; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY chat_sinbin
    ADD CONSTRAINT fk_chat_sinbin_empire FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_colony_empire; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY colonies
    ADD CONSTRAINT fk_colony_empire FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_colony_sector; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY colonies
    ADD CONSTRAINT fk_colony_sector FOREIGN KEY (sector_id) REFERENCES sectors(id);


--
-- Name: fk_colony_star; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY colonies
    ADD CONSTRAINT fk_colony_star FOREIGN KEY (star_id) REFERENCES stars(id);


--
-- Name: fk_combat_reports_stars; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY combat_reports
    ADD CONSTRAINT fk_combat_reports_stars FOREIGN KEY (star_id) REFERENCES stars(id);


--
-- Name: fk_conversation_participant_conversations; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY chat_conversation_participants
    ADD CONSTRAINT fk_conversation_participant_conversations FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id);


--
-- Name: fk_conversation_participant_empires; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY chat_conversation_participants
    ADD CONSTRAINT fk_conversation_participant_empires FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_empire_cash_audit_empire; Type: FK CONSTRAINT; Schema: beta; Owner: postgres
--

ALTER TABLE ONLY empire_cash_audit
    ADD CONSTRAINT fk_empire_cash_audit_empire FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_empire_rank_histories_empire; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY empire_rank_histories
    ADD CONSTRAINT fk_empire_rank_histories_empire FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_empire_shields_empire; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY empire_shields
    ADD CONSTRAINT fk_empire_shields_empire FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_empires_alliances; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY empires
    ADD CONSTRAINT fk_empires_alliances FOREIGN KEY (alliance_id) REFERENCES alliances(id);


--
-- Name: fk_empires_home_star; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY empires
    ADD CONSTRAINT fk_empires_home_star FOREIGN KEY (home_star_id) REFERENCES stars(id);


--
-- Name: fk_fleet_upgrade_fleet; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY fleet_upgrades
    ADD CONSTRAINT fk_fleet_upgrade_fleet FOREIGN KEY (fleet_id) REFERENCES fleets(id);


--
-- Name: fk_fleet_upgrade_star; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY fleet_upgrades
    ADD CONSTRAINT fk_fleet_upgrade_star FOREIGN KEY (star_id) REFERENCES stars(id);


--
-- Name: fk_fleets_empire; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY fleets
    ADD CONSTRAINT fk_fleets_empire FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_fleets_sector; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY fleets
    ADD CONSTRAINT fk_fleets_sector FOREIGN KEY (sector_id) REFERENCES sectors(id);


--
-- Name: fk_fleets_star; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY fleets
    ADD CONSTRAINT fk_fleets_star FOREIGN KEY (star_id) REFERENCES stars(id);


--
-- Name: fk_fleets_target_fleet; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY fleets
    ADD CONSTRAINT fk_fleets_target_fleet FOREIGN KEY (target_fleet_id) REFERENCES fleets(id);


--
-- Name: fk_fleets_target_star; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY fleets
    ADD CONSTRAINT fk_fleets_target_star FOREIGN KEY (target_star_id) REFERENCES stars(id);


--
-- Name: fk_purchases_empire; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY purchases
    ADD CONSTRAINT fk_purchases_empire FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_scout_reports_empires; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY scout_reports
    ADD CONSTRAINT fk_scout_reports_empires FOREIGN KEY (empire_id) REFERENCES empires(id);


--
-- Name: fk_scout_reports_stars; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY scout_reports
    ADD CONSTRAINT fk_scout_reports_stars FOREIGN KEY (star_id) REFERENCES stars(id);


--
-- Name: fk_star_renames_stars; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY star_renames
    ADD CONSTRAINT fk_star_renames_stars FOREIGN KEY (star_id) REFERENCES stars(id);


--
-- Name: fk_stars_sectors; Type: FK CONSTRAINT; Schema: beta; Owner: wwmmo_user
--

ALTER TABLE ONLY stars
    ADD CONSTRAINT fk_stars_sectors FOREIGN KEY (sector_id) REFERENCES sectors(id);


--
-- Name: beta; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA beta FROM PUBLIC;
REVOKE ALL ON SCHEMA beta FROM postgres;
GRANT ALL ON SCHEMA beta TO postgres;
GRANT ALL ON SCHEMA beta TO wwmmo WITH GRANT OPTION;


--
-- Name: empire_cash_audit; Type: ACL; Schema: beta; Owner: postgres
--

REVOKE ALL ON TABLE empire_cash_audit FROM PUBLIC;
REVOKE ALL ON TABLE empire_cash_audit FROM postgres;
GRANT ALL ON TABLE empire_cash_audit TO postgres;
GRANT ALL ON TABLE empire_cash_audit TO wwmmo;


--
-- Name: empire_cash_audit_id_seq; Type: ACL; Schema: beta; Owner: postgres
--

REVOKE ALL ON SEQUENCE empire_cash_audit_id_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE empire_cash_audit_id_seq FROM postgres;
GRANT ALL ON SEQUENCE empire_cash_audit_id_seq TO postgres;
GRANT ALL ON SEQUENCE empire_cash_audit_id_seq TO wwmmo;


--
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: beta; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA beta REVOKE ALL ON SEQUENCES  FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA beta REVOKE ALL ON SEQUENCES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA beta GRANT ALL ON SEQUENCES  TO wwmmo;


--
-- Name: DEFAULT PRIVILEGES FOR TYPES; Type: DEFAULT ACL; Schema: beta; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA beta REVOKE ALL ON TYPES  FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA beta REVOKE ALL ON TYPES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA beta GRANT ALL ON TYPES  TO wwmmo;


--
-- Name: DEFAULT PRIVILEGES FOR FUNCTIONS; Type: DEFAULT ACL; Schema: beta; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA beta REVOKE ALL ON FUNCTIONS  FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA beta REVOKE ALL ON FUNCTIONS  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA beta GRANT ALL ON FUNCTIONS  TO wwmmo;


--
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: beta; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA beta REVOKE ALL ON TABLES  FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA beta REVOKE ALL ON TABLES  FROM postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA beta GRANT ALL ON TABLES  TO wwmmo;


--
-- PostgreSQL database dump complete
--

