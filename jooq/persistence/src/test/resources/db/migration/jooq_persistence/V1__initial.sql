CREATE TABLE lib_processed_intents (
    intent_id BIGINT NOT NULL,
    path VARCHAR(255) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
    hash BIGINT NOT NULL,
    PRIMARY KEY (intent_id, path, hash)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE author (
    id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    nationality CHAR(1) NOT NULL,
    PRIMARY KEY (id)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE author_extra (
    id BIGINT NOT NULL,
    extra VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY fk_author_extra__id (id) REFERENCES author (id)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE book (
    id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_book__author_id_name (author_id, name),
    FOREIGN KEY fk_book__author_id (author_id) REFERENCES author (id)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE article (
    id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    published_date BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_article__author_id_name (author_id, name),
    FOREIGN KEY fk_article__author_id (author_id) REFERENCES author (id)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE article_related (
    id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    related VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_article_related__article_id_related (article_id, related),
    FOREIGN KEY fk_article_related__article_id (article_id) REFERENCES article (id)
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;
