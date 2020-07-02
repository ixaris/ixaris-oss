CREATE TABLE test_table (
  name VARCHAR(50) NOT NULL,
  last_updated BIGINT NOT NULL,
  flag1 CHAR(1) NOT NULL,
  flag2 TINYINT UNSIGNED NOT NULL,
  data VARCHAR(255) NOT NULL, -- TODO change to JSON after upgrade to mysql 5.7+
  PRIMARY KEY (`name`)
);
