CREATE TABLE `man` (
  `id` BIGINT(20) NOT NULL,
  `name` VARCHAR(30) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `stache` (
  `id` bigint(20) NOT NULL,
  `name` varchar(30) DEFAULT NULL,
  `ownerId` BIGINT(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `beard` (
  `id` bigint(20) NOT NULL,
  `name` varchar(30) DEFAULT NULL,
  `ownerId` BIGINT(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
