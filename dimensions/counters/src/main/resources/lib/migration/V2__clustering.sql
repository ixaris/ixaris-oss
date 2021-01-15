ALTER TABLE lib_dim_counter
    CHANGE COLUMN narrow_window_width narrow_window_width SMALLINT UNSIGNED NOT NULL,
    CHANGE COLUMN narrow_window_unit narrow_window_unit CHAR(1) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
    CHANGE COLUMN wide_window_multiple wide_window_multiple SMALLINT UNSIGNED NOT NULL,
    ADD COLUMN shard SMALLINT UNSIGNED NOT NULL,
    DROP INDEX ix_lib_dim_counter__queried_updated_diff,
    ADD KEY ix_lib_dim_counter__queried_updated_diff_shard (queried_updated_diff, shard);

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

INSERT INTO lib_dim_counter_event_queue (counter_key, event_id, counter_affected, dimension_name, dimension_long_value, dimension_string_value, shard)
SELECT counter_key, event_id, counter_affected, dimension_name, dimension_long_value, dimension_string_value, 0 
FROM lib_dim_counter_event
ORDER BY id;

DROP TABLE lib_dim_counter_event;
