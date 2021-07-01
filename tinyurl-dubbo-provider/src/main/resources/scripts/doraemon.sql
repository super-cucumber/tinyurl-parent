
-- 导出 doraemon 的数据库结构
CREATE DATABASE IF NOT EXISTS `doraemon`;
USE `doraemon`;


-- 导出  表 doraemon.flicker_id 结构
CREATE TABLE IF NOT EXISTS `flicker_id` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `gmt_create` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 正在导出表  doraemon.flicker_id 的数据：~0 rows (大约)
DELETE FROM `flicker_id`;


-- 导出  表 doraemon.leaf_alloc 结构
CREATE TABLE IF NOT EXISTS `leaf_alloc` (
  `biz_tag` varchar(128) NOT NULL DEFAULT '',
  `max_id` bigint(20) NOT NULL DEFAULT '1',
  `step` int(11) NOT NULL,
  `description` varchar(256) DEFAULT NULL,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`biz_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 正在导出表  doraemon.leaf_alloc 的数据：~3 rows (大约)
DELETE FROM `leaf_alloc`;
INSERT INTO `leaf_alloc` (`biz_tag`, `max_id`, `step`, `description`, `update_time`) VALUES
	('q.vipgp88.com', 3015, 15000, 'q', '2021-06-02 11:11:50'),
	('t.vipgp88.com', 11996705, 15000, 't', '2021-06-02 22:36:44'),
	('tinyuurl', 40010, 1000, 'tinyuurl', '2021-04-22 01:17:04');



-- 导出  表 doraemon.lookup 结构
CREATE TABLE IF NOT EXISTS `lookup` (
  `lookup_id` int(11) NOT NULL AUTO_INCREMENT,
  `lookup_key` varchar(25) NOT NULL,
  `lookup_value` varchar(100) NOT NULL,
  `gmt_create` datetime NOT NULL,
  `gmt_modified` datetime DEFAULT NULL,
  PRIMARY KEY (`lookup_id`),
  UNIQUE KEY `unique_key` (`lookup_key`),
  UNIQUE KEY `unique_value` (`lookup_value`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

-- 正在导出表  doraemon.lookup 的数据：~2 rows (大约)
DELETE FROM `lookup`;
INSERT INTO `lookup` (`lookup_id`, `lookup_key`, `lookup_value`, `gmt_create`, `gmt_modified`) VALUES
	(1, 't', 't.vipgp88.com', '2021-03-15 12:00:00', NULL),
	(2, 'q', 'q.vipgp88.com', '2021-03-15 12:00:00', NULL);


-- 导出  表 doraemon.tiny_raw_url_rel_0 结构
CREATE TABLE IF NOT EXISTS `tiny_raw_url_rel_0` (
  `id` bigint(20) unsigned NOT NULL COMMENT '主键ID',
  `base_url` char(1) NOT NULL DEFAULT '1' COMMENT '基础url   1-t.com  2-q.com',
  `tiny_url` varchar(64) NOT NULL COMMENT '短url',
  `raw_url` varchar(2048) NOT NULL COMMENT '原始url',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`,`base_url`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='tiny url and raw url relation';

-- 正在导出表  doraemon.tiny_raw_url_rel_0 的数据：~0 rows (大约)
DELETE FROM `tiny_raw_url_rel_0`;


-- 导出  表 doraemon.tiny_raw_url_rel_1 结构
CREATE TABLE IF NOT EXISTS `tiny_raw_url_rel_1` (
  `id` bigint(20) unsigned NOT NULL COMMENT '主键ID',
  `base_url` char(1) NOT NULL DEFAULT '1' COMMENT '基础url   1-t.com  2-q.com',
  `tiny_url` varchar(64) NOT NULL COMMENT '短url',
  `raw_url` varchar(2048) NOT NULL COMMENT '原始url',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`,`base_url`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='tiny url and raw url relation';

-- 正在导出表  doraemon.tiny_raw_url_rel_1 的数据：~0 rows (大约)
DELETE FROM `tiny_raw_url_rel_1`;


-- 导出  表 doraemon.tiny_raw_url_rel_2 结构
CREATE TABLE IF NOT EXISTS `tiny_raw_url_rel_2` (
  `id` bigint(20) unsigned NOT NULL COMMENT '主键ID',
  `base_url` char(1) NOT NULL DEFAULT '1' COMMENT '基础url   1-t.com  2-q.com',
  `tiny_url` varchar(64) NOT NULL COMMENT '短url',
  `raw_url` varchar(2048) NOT NULL COMMENT '原始url',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`,`base_url`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='tiny url and raw url relation';

-- 正在导出表  doraemon.tiny_raw_url_rel_2 的数据：~0 rows (大约)
DELETE FROM `tiny_raw_url_rel_2`;

