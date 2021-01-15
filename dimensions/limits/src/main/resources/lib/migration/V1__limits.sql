CREATE TABLE lib_dim_limit (
    id BIGINT NOT NULL,
    limit_key VARCHAR(50) NOT NULL,
    context_depth BIGINT NOT NULL,
    max_count BIGINT DEFAULT NULL,
    min_amount BIGINT DEFAULT NULL,
    max_amount BIGINT DEFAULT NULL,
    narrow_window_width SMALLINT DEFAULT NULL,
    narrow_window_unit CHAR(1) DEFAULT NULL,
    wide_window_multiple SMALLINT DEFAULT NULL,
    effective_from BIGINT NOT NULL,
    effective_to BIGINT DEFAULT NULL,
    last_updated BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY ix_lib_dim_limit__limit_key_context_depth (limit_key, context_depth),
    KEY ix_lib_dim_limit__effective (limit_key, effective_from, effective_to)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE lib_dim_limit_dimension (
    id BIGINT NOT NULL,
    dimension_name VARCHAR(60) NOT NULL,
    long_value BIGINT DEFAULT NULL,
    string_value VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id, dimension_name),
    CONSTRAINT fk_lib_dim_limit_dimension__id FOREIGN KEY (id) REFERENCES lib_dim_limit(id),
    KEY ix_lib_dim_limit_dimension__name_long_value (dimension_name, long_value),
    KEY ix_lib_dim_limit_dimension__name_string_value (dimension_name, string_value(32))
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;
