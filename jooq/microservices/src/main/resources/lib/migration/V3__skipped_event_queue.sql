CREATE TABLE lib_handle_event_queue (
    sequence_number INT UNSIGNED NOT NULL AUTO_INCREMENT,
    listener_name VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
    event_envelope BLOB,
    shard SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    failure_count SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    next_retry_time BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (sequence_number),
    KEY ix_lib_handle_event_queue__shard_listener_name (shard, listener_name),
    KEY ix_lib_handle_event_queue__next_retry_time (next_retry_time)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;
