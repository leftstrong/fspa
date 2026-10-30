-- 智能换物：改为「只给方案 + 只走第一棒」，路径规划费在成交时结算
-- 1) 换物单：记录本单需结算的路径费
ALTER TABLE `eb_barter_order`
  ADD COLUMN `path_fee_points`  INT     NOT NULL DEFAULT 0 COMMENT '路径规划费应收积分（0=普通换物单）' AFTER `chain_size`,
  ADD COLUMN `path_fee_charged` INT     NOT NULL DEFAULT 0 COMMENT '路径规划费实扣积分（余额不足时记录实际扣到的数）' AFTER `path_fee_points`,
  ADD COLUMN `path_fee_paid`    TINYINT NOT NULL DEFAULT 0 COMMENT '路径费是否已结算：0 未结算 / 1 已结算' AFTER `path_fee_charged`;

-- 2) 物品：标记「这件是我通过换物接力收到的」，用于「继续换下一件」
ALTER TABLE `eb_barter_item`
  ADD COLUMN `receive_from_order_id` INT NULL COMMENT '通过哪张换物单收到（非空=接力收到的物品，可重新上架）' AFTER `delivery_type`;

-- 3) 历史环单不再需要路径费（它们已经在成环时扣过分）
UPDATE `eb_barter_order` SET `path_fee_points` = 0, `path_fee_charged` = 0, `path_fee_paid` = 1
WHERE `chain_no` IS NOT NULL AND `chain_no` <> '';
