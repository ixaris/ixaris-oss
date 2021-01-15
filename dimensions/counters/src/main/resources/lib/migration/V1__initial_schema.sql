CREATE TABLE lib_dim_counter (
    id BIGINT NOT NULL,
    narrow_window_width SMALLINT NOT NULL,
    narrow_window_unit CHAR(1) NOT NULL,
    wide_window_multiple SMALLINT NOT NULL,
    start_narrow_window_number BIGINT NOT NULL,
    count BIGINT NOT NULL,
    sum BIGINT NOT NULL,
    last_queried BIGINT NOT NULL,
    queried_updated_diff BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY ix_lib_dim_counter__window (narrow_window_width, narrow_window_unit, wide_window_multiple),
    KEY ix_lib_dim_counter__last_queried (last_queried),
    KEY ix_lib_dim_counter__queried_updated_diff (queried_updated_diff)
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

CREATE TABLE lib_dim_counter_event (
    id BIGINT NOT NULL,
    counter_key VARCHAR(50) NOT NULL,
    event_id BIGINT NOT NULL,
    counter_affected TINYINT UNSIGNED NOT NULL,
    dimension_name VARCHAR(50) DEFAULT NULL,
    dimension_long_value BIGINT DEFAULT NULL,
    dimension_string_value VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY ix_lib_dim_counter_pending_event__counter_key_dimension_name (counter_key, dimension_name)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;