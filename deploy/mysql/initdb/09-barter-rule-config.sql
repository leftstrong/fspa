-- =====================================================================
-- 以物换物 增量：规则配置项补齐（积分 / 智能换物路径费 / 信用分 / 履约时效）
-- 幂等：按 name 判断，已存在则跳过，可重复执行
-- 说明：这些键即使不在表中，后端 BarterConfigServiceImpl 也会回落到内置默认值；
--       写入表中的目的是让后台「规则配置」页有明确初始值，且便于统一维护。
-- =====================================================================
USE `crmeb`;

DROP PROCEDURE IF EXISTS `tmp_add_barter_config`;
DELIMITER $$
CREATE PROCEDURE `tmp_add_barter_config`()
BEGIN
    -- ---------- 积分规则 ----------
    INSERT INTO eb_system_config (name, title, value, form_id, status, create_time, update_time)
    SELECT 'barter_points_register', '换物积分-新用户注册奖励', '50', 0, 0, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'barter_points_register');

    INSERT INTO eb_system_config (name, title, value, form_id, status, create_time, update_time)
    SELECT 'barter_points_first_publish', '换物积分-首发好物奖励（审核通过发放，每人一次）', '50', 0, 0, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'barter_points_first_publish');

    INSERT INTO eb_system_config (name, title, value, form_id, status, create_time, update_time)
    SELECT 'barter_points_per_deal', '换物积分-成交奖励（双方各得）', '10', 0, 0, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'barter_points_per_deal');

    -- ---------- 智能换物路径规划费 ----------
    INSERT INTO eb_system_config (name, title, value, form_id, status, create_time, update_time)
    SELECT 'barter_path_fee_2p', '智能换物-路径费（2 人直接互换）', '0', 0, 0, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'barter_path_fee_2p');

    INSERT INTO eb_system_config (name, title, value, form_id, status, create_time, update_time)
    SELECT 'barter_path_fee_3p', '智能换物-路径费（3 人接力）', '10', 0, 0, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'barter_path_fee_3p');

    INSERT INTO eb_system_config (name, title, value, form_id, status, create_time, update_time)
    SELECT 'barter_path_fee_4_6p', '智能换物-路径费（4-6 人接力）', '50', 0, 0, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'barter_path_fee_4_6p');

    INSERT INTO eb_system_config (name, title, value, form_id, status, create_time, update_time)
    SELECT 'barter_chain_max_hops', '智能换物-最大跳数（跳数 = 人数 - 1）', '5', 0, 0, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'barter_chain_max_hops');

    -- ---------- 信用分 ----------
    INSERT INTO eb_system_config (name, title, value, form_id, status, create_time, update_time)
    SELECT 'barter_credit_per_deal', '信用分-换物成交加分', '5', 0, 0, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'barter_credit_per_deal');

    INSERT INTO eb_system_config (name, title, value, form_id, status, create_time, update_time)
    SELECT 'barter_credit_good', '信用分-互评好评加分（4-5 星）', '2', 0, 0, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'barter_credit_good');

    INSERT INTO eb_system_config (name, title, value, form_id, status, create_time, update_time)
    SELECT 'barter_credit_bad', '信用分-互评差评扣分（1-2 星）', '3', 0, 0, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'barter_credit_bad');

    -- ---------- 履约时效 ----------
    INSERT INTO eb_system_config (name, title, value, form_id, status, create_time, update_time)
    SELECT 'barter_express_edit_hours', '履约-发货后可自行改单号时长（小时）', '24', 0, 0, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'barter_express_edit_hours');

    INSERT INTO eb_system_config (name, title, value, form_id, status, create_time, update_time)
    SELECT 'barter_express_edit_max', '履约-单侧最多可改单号次数', '3', 0, 0, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name = 'barter_express_edit_max');
END$$
DELIMITER ;

CALL `tmp_add_barter_config`();
DROP PROCEDURE IF EXISTS `tmp_add_barter_config`;

-- 核对结果
SELECT name, title, value FROM eb_system_config WHERE name LIKE 'barter%' ORDER BY name;
