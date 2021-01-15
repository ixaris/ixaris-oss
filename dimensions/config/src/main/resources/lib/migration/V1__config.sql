CREATE TABLE lib_dim_config_value (
    id BIGINT NOT NULL,
    value_key VARCHAR(50) NOT NULL,
    context_depth BIGINT NOT NULL,
    long_value BIGINT DEFAULT NULL,
    string_value VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY ix_lib_dim_config_value__value_key_context_depth (value_key, context_depth),
    KEY ix_lib_dim_config_value__long_value_string_value (long_value, string_value(32))
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE lib_dim_config_value_dimension (
    id BIGINT NOT NULL,
    dimension_name VARCHAR(60) NOT NULL,
    long_value BIGINT DEFAULT NULL,
    string_value VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id, dimension_name),
    CONSTRAINT fk_lib_dim_config_value_dimension__id FOREIGN KEY (id) REFERENCES lib_dim_config_value(id),
    KEY ix_lib_dim_config_value_dimension__name_long_value (dimension_name, long_value),
    KEY ix_lib_dim_config_value_dimension__name_string_value (dimension_name, string_value(32))
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE lib_dim_config_set (
    id BIGINT NOT NULL,
    set_key VARCHAR(50) NOT NULL,
    context_depth BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY ix_lib_dim_config_set__set_key_context_depth (set_key, context_depth)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE lib_dim_config_set_value (
    id BIGINT NOT NULL,
    long_value BIGINT DEFAULT NULL,
    string_value VARCHAR(255) DEFAULT NULL,
    CONSTRAINT fk_lib_dim_config_set_value__id FOREIGN KEY (id) REFERENCES lib_dim_config_set(id),
    UNIQUE KEY pk_lib_dim_config_set_value__long_value_string_value_id (long_value, string_value, id)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE lib_dim_config_set_dimension (
    id BIGINT NOT NULL,
    dimension_name VARCHAR(60) NOT NULL,
    long_value BIGINT DEFAULT NULL,
    string_value VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id, dimension_name),
    CONSTRAINT fk_lib_dim_config_set_dimension__id FOREIGN KEY (id) REFERENCES lib_dim_config_set(id),
    KEY ix_lib_dim_config_set_dimension__name_long_value (dimension_name, long_value),
    KEY ix_lib_dim_config_set_dimension__name_string_value (dimension_name, string_value(32))
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;
