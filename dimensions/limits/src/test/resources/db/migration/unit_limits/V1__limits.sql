CREATE TABLE lib_dim_limit (
    id BIGINT NOT NULL,
    limit_key VARCHAR(50) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
    context_depth BIGINT NOT NULL,
    max_count BIGINT DEFAULT NULL,
    min_amount BIGINT DEFAULT NULL,
    max_amount BIGINT DEFAULT NULL,
    narrow_window_width SMALLINT UNSIGNED DEFAULT NULL,
    narrow_window_unit CHAR(1) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,
    wide_window_multiple SMALLINT UNSIGNED DEFAULT NULL,
    effective_from BIGINT NOT NULL,
    effective_to BIGINT DEFAULT NULL,
    last_updated BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY ix_lib_dim_limit__limit_key_context_depth (limit_key, context_depth),
    KEY ix_lib_dim_limit__effective (limit_key, effective_from, effective_to)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE lib_dim_limit_dimension (
    id BIGINT NOT NULL,
    dimension_name VARCHAR(50) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
    long_value BIGINT DEFAULT NULL,
    string_value VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,
    PRIMARY KEY (id, dimension_name),
    CONSTRAINT fk_lib_dim_limit_dimension__id FOREIGN KEY (id) REFERENCES lib_dim_limit(id),
    KEY ix_lib_dim_limit_dimension__name_long_value (dimension_name, long_value),
    KEY ix_lib_dim_limit_dimension__name_string_value (dimension_name, string_value(32))
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE lib_dim_counter (
    id BIGINT NOT NULL,
    narrow_window_width SMALLINT UNSIGNED NOT NULL,
    narrow_window_unit CHAR(1) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
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
    next_retry_time BIGINT NOT NULL  DEFAULT 0,
    PRIMARY KEY (sequence_number),
    KEY ix_lib_dim_counter_event_queue__shard (shard),
    KEY ix_lib_dim_counter_event_queue__next_retry_time (next_retry_time)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE test_event (
    id BIGINT NOT NULL,
    timestamp BIGINT NOT NULL,
    delta BIGINT NOT NULL,
    counter_affected TINYINT UNSIGNED NOT NULL,
    long1 BIGINT DEFAULT NULL,
    long2 BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    KEY ix_test_event__long1_long2 (long1, long2)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE test_event_counter_dimensions (
    id BIGINT NOT NULL,
    long1 BIGINT DEFAULT NULL,
    long2 BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_test_event_counter_dimensions__id FOREIGN KEY (id) REFERENCES lib_dim_counter(id),
    KEY ix_test_event_counter_dimensions__long1_long2 (long1, long2)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;
