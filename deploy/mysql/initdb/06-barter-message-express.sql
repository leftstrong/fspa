-- 良物换物 · 站内消息 + 快递公司字段（幂等可执行）

-- 1. 站内消息表
CREATE TABLE IF NOT EXISTS `eb_barter_message` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uid` int(11) NOT NULL COMMENT '接收者',
  `type` tinyint(4) NOT NULL DEFAULT 0 COMMENT '消息类型 1收到申请 2申请被接受 3申请被拒绝 4申请被取消 5换物单生成 6对方已发货 7对方已收货 8换物完成 9有人想要 10系统',
  `group_type` varchar(20) NOT NULL DEFAULT 'system' COMMENT '分组 incoming/outgoing/order/want/system',
  `biz_id` int(11) DEFAULT NULL COMMENT '关联业务ID',
  `title` varchar(100) DEFAULT '' COMMENT '标题',
  `content` varchar(500) DEFAULT '' COMMENT '正文',
  `is_read` tinyint(4) NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  `create_time` datetime DEFAULT NULL,
  `read_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_uid_read` (`uid`,`is_read`),
  KEY `idx_uid_group` (`uid`,`group_type`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='换物站内消息';

-- 2. 换物单加快递公司字段
SET @exist := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eb_barter_order' AND COLUMN_NAME = 'owner_logistics_company');
SET @sql := IF(@exist = 0,
  'ALTER TABLE eb_barter_order ADD COLUMN owner_logistics_company varchar(32) DEFAULT '''' COMMENT ''主人发货快递公司编码'' AFTER owner_logistics_no',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eb_barter_order' AND COLUMN_NAME = 'apply_logistics_company');
SET @sql := IF(@exist = 0,
  'ALTER TABLE eb_barter_order ADD COLUMN apply_logistics_company varchar(32) DEFAULT '''' COMMENT ''申请人发货快递公司编码'' AFTER apply_logistics_no',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 换物单加地址快照字段（幂等，兼容早期版本）
SET @exist := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eb_barter_order' AND COLUMN_NAME = 'owner_address');
SET @sql := IF(@exist = 0,
  'ALTER TABLE eb_barter_order ADD COLUMN owner_address varchar(255) DEFAULT NULL COMMENT ''主人收货地址快照'' AFTER self_take_address',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eb_barter_order' AND COLUMN_NAME = 'apply_address');
SET @sql := IF(@exist = 0,
  'ALTER TABLE eb_barter_order ADD COLUMN apply_address varchar(255) DEFAULT NULL COMMENT ''申请人收货地址快照'' AFTER owner_address',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
