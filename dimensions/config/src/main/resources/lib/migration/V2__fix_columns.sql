ALTER TABLE lib_dim_config_value
    CHANGE COLUMN value_key value_key VARCHAR(50) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
    CHANGE COLUMN string_value string_value VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL;

ALTER TABLE lib_dim_config_value_dimension
    CHANGE COLUMN dimension_name dimension_name VARCHAR(50) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
    CHANGE COLUMN string_value string_value VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL;
    
ALTER TABLE lib_dim_config_set
    CHANGE COLUMN set_key set_key VARCHAR(50) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL;
    
ALTER TABLE lib_dim_config_set_value
    CHANGE COLUMN string_value string_value VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL;
    
ALTER TABLE lib_dim_config_set_dimension
    CHANGE COLUMN dimension_name dimension_name VARCHAR(50) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
    CHANGE COLUMN string_value string_value VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL;
    