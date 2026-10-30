-- ============================================================
-- 良物 · 以物换物 后台菜单 & 系统配置种子
-- 主题：物为人用，方为良物
-- 说明：
--   1) eb_system_menu：父菜单「良物换物」+ 6 个子菜单，
--      component 路径必须与前端路由完全一致（见 fspa_vue/src/router/modules/barter.js）。
--   2) eb_system_config：置顶积分 / 超时自动关闭等可后台配置项，
--      后端以 getValueByKey 读取，未配置时走代码默认值。
-- 执行方式：直接进入对应数据库后 source 本文件，或用客户端导入。
-- ============================================================

-- ----------------------------
-- 1. 后台菜单
-- ----------------------------
INSERT INTO `eb_system_menu` (`id`, `pid`, `name`, `icon`, `perms`, `component`, `menu_type`, `sort`, `is_show`, `is_delte`, `create_time`, `update_time`) VALUES
(9000, 0, '良物换物', 's-management', NULL, '/barter', 'M', 11, 1, 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00'),
(9001, 9000, '物品审核', NULL, 'admin:barter:item:list', '/barter/item/index', 'C', 1, 1, 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00'),
(9002, 9000, '换物订单', NULL, 'admin:barter:order:list', '/barter/order/index', 'C', 2, 1, 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00'),
(9003, 9000, '会员等级', NULL, 'admin:barter:level:list', '/barter/level/index', 'C', 3, 1, 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00'),
(9004, 9000, '品类管理', NULL, 'admin:barter:category:list', '/barter/category/index', 'C', 4, 1, 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00'),
(9005, 9000, '举报处理', NULL, 'admin:barter:report:list', '/barter/report/index', 'C', 5, 1, 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00'),
(9006, 9000, '规则配置', NULL, 'admin:barter:config:list', '/barter/config/index', 'C', 6, 1, 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00'),
(9007, 9000, '信用积分看板', NULL, 'admin:barter:user:stat', '/barter/user/index', 'C', 7, 1, 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00');

-- ----------------------------
-- 2. 系统配置（置顶积分 / 超时等，可后台编辑）
--    name = 后端读取的 key，value = 默认值
-- ----------------------------
INSERT INTO `eb_system_config` (`name`, `title`, `form_id`, `value`, `status`, `create_time`, `update_time`) VALUES
('barter_top_points_per_hour', '换物置顶-每积分（置顶1小时所需积分）', 0, '100', 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00'),
('barter_top_duration_hour', '换物置顶-时长（小时）', 0, '1', 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00'),
('barter_apply_expire_hour', '换物申请-超时未确认自动关闭（小时）', 0, '48', 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00'),
('barter_recv_expire_hour', '换物单-超时未确认收货自动完成（小时）', 0, '168', 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00');

-- 微信订阅消息模板 ID（留空则由后端忽略推送，配置后自动生效）
INSERT INTO `eb_system_config` (`name`, `title`, `form_id`, `value`, `status`, `create_time`, `update_time`) VALUES
('barter_tpl_apply_created', '换物-申请创建通知模板ID', 0, '', 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00'),
('barter_tpl_apply_accepted', '换物-申请通过通知模板ID', 0, '', 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00'),
('barter_tpl_order_delivered', '换物-已发货通知模板ID', 0, '', 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00'),
('barter_tpl_order_completed', '换物-已完成通知模板ID', 0, '', 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00'),
('barter_tpl_order_cancelled', '换物-已取消通知模板ID', 0, '', 0, '2025-06-01 00:00:00', '2025-06-01 00:00:00');
