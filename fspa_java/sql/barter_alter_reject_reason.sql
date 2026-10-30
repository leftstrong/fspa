-- =====================================================================
-- 以物换物 增量：eb_barter_item 增加 reject_reason（驳回原因）
-- 幂等：可重复执行（已存在则跳过）
-- 背景：审核驳回时由后台填写原因，便于审核记录追溯。
-- =====================================================================
USE `crmeb`;

-- ---------------------------------------------------------------------
-- eb_barter_item 增加 reject_reason 列
-- ---------------------------------------------------------------------
SET @s = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'eb_barter_item' AND column_name = 'reject_reason') > 0,
  'SELECT ''column reject_reason already exists'' AS msg',
  'ALTER TABLE eb_barter_item ADD COLUMN reject_reason VARCHAR(255) NOT NULL DEFAULT '''' COMMENT ''驳回原因'' AFTER like_count'
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
