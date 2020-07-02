CREATE TABLE lib_processed_intents (
    intent_id BIGINT NOT NULL,
    path VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
    hash BIGINT NOT NULL,
    PRIMARY KEY (intent_id, path, hash)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;