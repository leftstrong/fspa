-- =====================================================================
-- 以物换物 增量：「我想要」点赞 + 收货地址 + 用户城市
-- 幂等：可重复执行
-- =====================================================================
USE `crmeb`;

-- ---------------------------------------------------------------------
-- 1. 「我想要」记录表（点赞）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `eb_barter_item_like`;
CREATE TABLE `eb_barter_item_like` (
  `id`          INT(11) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `item_id`     INT(11) NOT NULL DEFAULT 0 COMMENT '物品ID',
  `uid`         INT(11) NOT NULL DEFAULT 0 COMMENT '用户ID',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_uid` (`item_id`, `uid`),
  KEY `idx_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='我想要（点赞）记录表';

-- ---------------------------------------------------------------------
-- 2. eb_barter_item 增加 want_count 冗余计数（避免每次 COUNT）
-- ---------------------------------------------------------------------
SET @s = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'eb_barter_item' AND column_name = 'want_count') > 0,
  'SELECT ''column want_count already exists'' AS msg',
  'ALTER TABLE eb_barter_item ADD COLUMN want_count INT(11) NOT NULL DEFAULT 0 COMMENT ''想要人数'' AFTER like_count'
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 3. 收货地址表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `eb_barter_user_address`;
CREATE TABLE `eb_barter_user_address` (
  `id`          INT(11)      NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `uid`         INT(11)      NOT NULL DEFAULT 0 COMMENT '用户ID',
  `name`        VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '收货人姓名',
  `phone`       VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '收货人电话',
  `province`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '省',
  `city`        VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '市',
  `district`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '区/县',
  `detail`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT '详细地址',
  `is_default`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否默认：0否 1是',
  `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='换物收货地址表';

-- ---------------------------------------------------------------------
-- 4. eb_user 增加 barter_city（用户所在城市）
-- ---------------------------------------------------------------------
SET @s = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'eb_user' AND column_name = 'barter_city') > 0,
  'SELECT ''column barter_city already exists'' AS msg',
  'ALTER TABLE eb_user ADD COLUMN barter_city VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''换物所在城市'' AFTER barter_level_id'
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 5. 初始化 want_count（按历史 like 记录回填，防止 NULL 语义歧义）
-- ---------------------------------------------------------------------
UPDATE eb_barter_item i SET i.want_count = (
  SELECT COUNT(*) FROM eb_barter_item_like l WHERE l.item_id = i.id
) WHERE i.want_count = 0;
