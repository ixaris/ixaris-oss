CREATE TABLE lib_dim_counter (
    id BIGINT NOT NULL,
    narrow_window_width SMALLINT UNSIGNED NOT NULL,
    narrow_window_unit CHAR(1) NOT NULL,
    wide_window_multiple SMALLINT UNSIGNED NOT NULL,
    start_narrow_window_number BIGINT NOT NULL,
    count BIGINT NOT NULL,
    sum BIGINT NOT NULL,
    last_queried BIGINT NOT NULL,
    queried_updated_diff BIGINT NOT NULL,
    shard SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    KEY ix_lib_dim_counter__window (narrow_window_width, narrow_window_unit, wide_window_multiple),
    KEY ix_lib_dim_counter__last_queried (last_queried),
    KEY ix_lib_dim_counter__queried_updated_diff_shard (queried_updated_diff, shard)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE lib_dim_counter_narrow (
    id BIGINT NOT NULL,
    counter_id BIGINT NOT NULL,
    window_number BIGINT NOT NULL,
    count BIGINT NOT NULL,
    sum BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY ix_lib_dim_counter_narrow__counter_id_window (counter_id, window_number),
    CONSTRAINT fk_lib_dim_counter_narrow__counter_id FOREIGN KEY (counter_id) REFERENCES lib_dim_counter(id)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE lib_dim_counter_event_queue (
    sequence_number INT UNSIGNED NOT NULL AUTO_INCREMENT,
    counter_key VARCHAR(50) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
    event_id BIGINT NOT NULL,
    counter_affected TINYINT UNSIGNED NOT NULL,
    dimension_name VARCHAR(50) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,
    dimension_long_value BIGINT DEFAULT NULL,
    dimension_string_value VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,
    shard SMALLINT UNSIGNED NOT NULL,
    tenant_id VARCHAR(50) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,
    intent_id BIGINT DEFAULT NULL,
    failure_count SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    next_retry_time BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (sequence_number),
    KEY ix_lib_dim_counter_event_queue__shard (shard),
    KEY ix_lib_dim_counter_event_queue__next_retry_time (next_retry_time)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE test_event (
    id BIGINT NOT NULL,
    timestamp BIGINT NOT NULL,
    delta BIGINT NOT NULL,
    counter_affected TINYINT UNSIGNED NOT NULL,
    a CHAR(3) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,
    b BIGINT DEFAULT NULL,
    c BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    KEY ix_test_event__a_b_c (a, b, c)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE test_event_counter_dimensions (
    id BIGINT NOT NULL,
    a CHAR(3) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,
    b BIGINT DEFAULT NULL,
    c BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_test_event_counter_dimensions__id FOREIGN KEY (id) REFERENCES lib_dim_counter(id),
    KEY ix_test_event_counter_dimensions__a_b (a, b),
    KEY ix_test_event_counter_dimensions__c (c)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;
