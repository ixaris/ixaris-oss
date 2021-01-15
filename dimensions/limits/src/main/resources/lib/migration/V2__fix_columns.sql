ALTER TABLE lib_dim_limit
    CHANGE COLUMN limit_key limit_key VARCHAR(50) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
    CHANGE COLUMN narrow_window_width narrow_window_width SMALLINT UNSIGNED DEFAULT NULL,
    CHANGE COLUMN narrow_window_unit narrow_window_unit CHAR(1) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,
    CHANGE COLUMN wide_window_multiple wide_window_multiple SMALLINT UNSIGNED DEFAULT NULL;
    
ALTER TABLE lib_dim_limit_dimension
    CHANGE COLUMN dimension_name dimension_name VARCHAR(50) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
    CHANGE COLUMN string_value string_value VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL;
