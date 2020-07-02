CREATE TABLE lib_envelope_queue (
    sequence_number BIGINT NOT NULL AUTO_INCREMENT,
    paths VARCHAR(512),
    event_envelope BLOB,
    PRIMARY KEY (sequence_number)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;