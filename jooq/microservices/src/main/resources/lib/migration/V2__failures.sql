RENAME TABLE lib_envelope_queue TO lib_publish_event_queue;

ALTER TABLE lib_publish_event_queue
    CHANGE COLUMN sequence_number sequence_number INT UNSIGNED NOT NULL AUTO_INCREMENT,
    CHANGE COLUMN paths path VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
    ADD COLUMN shard SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    ADD COLUMN failure_count SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    ADD COLUMN next_retry_time BIGINT NOT NULL DEFAULT 0,
    ADD KEY ix_lib_publish_event_queue__shard_path (shard, path),
    ADD KEY ix_lib_publish_event_queue__next_retry_time (next_retry_time);
