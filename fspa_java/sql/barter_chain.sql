-- =====================================================================
-- 良物 · 智能换物（多方交换环）增量脚本
-- 说明：在已有 eb_barter_order 表上增加「环号」相关字段，用于把一次成环生成的
--       N 张 1v1 换物单关联到一起；任一单被拒/取消时整环解散、物品释放回在售。
-- 幂等：每列单独判断是否已存在，可重复执行。
-- 执行：随 P1/P2 上线前执行一次（本脚本不删表、不动存量数据，老订单 chain_no 为空串即普通换物）。
-- =====================================================================

SET NAMES utf8mb4;

-- 1) chain_no：环号
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eb_barter_order' AND COLUMN_NAME = 'chain_no');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `eb_barter_order` ADD COLUMN `chain_no` VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''智能换物环号（空串=普通1v1换物）''',
  'SELECT ''chain_no 已存在，跳过'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2) chain_seq：本单在环上的序号（1 起）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eb_barter_order' AND COLUMN_NAME = 'chain_seq');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `eb_barter_order` ADD COLUMN `chain_seq` INT(11) NOT NULL DEFAULT 0 COMMENT ''本单在环上的序号（1起）''',
  'SELECT ''chain_seq 已存在，跳过'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 3) chain_size：环上人数（= 本次生成的订单张数 = 跳数+1）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eb_barter_order' AND COLUMN_NAME = 'chain_size');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `eb_barter_order` ADD COLUMN `chain_size` INT(11) NOT NULL DEFAULT 0 COMMENT ''环上人数（=订单张数）''',
  'SELECT ''chain_size 已存在，跳过'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 4) 环号索引（环详情按 chain_no 取 N 张单）
SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eb_barter_order' AND INDEX_NAME = 'idx_chain');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `eb_barter_order` ADD KEY `idx_chain` (`chain_no`)',
  'SELECT ''idx_chain 已存在，跳过'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 5) 积分日志关联类型补注：5 = 智能换物成环
ALTER TABLE `eb_barter_points_log`
  MODIFY COLUMN `link_type` TINYINT(1) NOT NULL DEFAULT 0
  COMMENT '关联类型：1置顶 2完成交换奖励 3发布奖励 4邀请 5智能换物成环';

-- 6) 自检：确认字段已就位
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eb_barter_order'
  AND COLUMN_NAME IN ('chain_no', 'chain_seq', 'chain_size');
