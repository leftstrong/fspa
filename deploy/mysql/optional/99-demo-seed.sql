-- 换物模块演示种子数据（可重复执行：先清理再插入）
SET NAMES utf8mb4;

-- 1. 额外两个演示用户（uid=2 林小满 / uid=3 陈拾）
DELETE FROM eb_user WHERE uid IN (2,3);
INSERT INTO eb_user (uid, account, pwd, real_name, nickname, avatar, phone, add_ip, last_ip, now_money, integral,
                     status, level, user_type, login_type, path, sex, country, create_time, update_time,
                     barter_exchange_count, barter_level_id, barter_points, credit_score)
VALUES
 (2, 'barter_demo_2', 'e10adc3949ba59abbe56e057f20f883e', '林小满', '林小满', 'crmebimage/public/maintain/2023/02/23/1c0fa967eb764d918f064744cc51dc70a6f2bj3sha.jpg',
  '13800000002', '127.0.0.1', '127.0.0.1', 0, 100, 1, 1, 'h5', 'h5', '/0/', 2, 'CN', NOW(), NOW(), 3, 3, 120, 95),
 (3, 'barter_demo_3', 'e10adc3949ba59abbe56e057f20f883e', '陈拾', '陈拾', 'crmebimage/public/maintain/2023/02/23/1c0fa967eb764d918f064744cc51dc70a6f2bj3sha.jpg',
  '13800000003', '127.0.0.1', '127.0.0.1', 0, 100, 1, 1, 'h5', 'h5', '/0/', 1, 'CN', NOW(), NOW(), 0, 1, 60, 80);

-- 演示用户等级也拉开一点
UPDATE eb_user SET barter_exchange_count = 6, barter_level_id = 4, barter_points = 260, credit_score = 98 WHERE uid = 1;

-- 2. 换物物品（先清掉演示数据）
DELETE FROM eb_barter_item WHERE id BETWEEN 9001 AND 9010;
INSERT INTO eb_barter_item (id, uid, category_id, category_pid, title, images, description, condition_level,
                            city, expect_type, expect_desc, expect_category_id, tags, status, top_end_time,
                            top_points, view_count, like_count, create_time, update_time)
VALUES
 (9001, 1, 1, 0, '《人类简史》九成新', 'crmebimage/public/maintain/2023/02/23/1c0fa967eb764d918f064744cc51dc70a6f2bj3sha.jpg',
  '搬家清书，只读过一遍，无笔记无折页。想换一本同样有意思的书。', 2, '深圳市', 2, '想换一本社科或历史类图书', 1,
  '图书,历史,搬家清仓', 1, NULL, 0, 128, 12, NOW(), NOW()),
 (9002, 2, 2, 0, 'Kindle Paperwhite 4 8G', 'crmebimage/public/maintain/2023/02/23/1c0fa967eb764d918f064744cc51dc70a6f2bj3sha.jpg',
  '屏幕无划痕，电池续航正常，附原装保护套。换一台闲置蓝牙键盘或同等数码小物。', 2, '深圳市', 2, '换蓝牙键盘/数码配件', 2,
  'kindle,数码,阅读器', 1, DATE_ADD(NOW(), INTERVAL 1 HOUR), 100, 356, 41, NOW(), NOW()),
 (9003, 3, 7, 0, '正版 猫咪手办 未拆封', 'crmebimage/public/maintain/2023/02/23/1c0fa967eb764d918f064744cc51dc70a6f2bj3sha.jpg',
  '抽奖重复了一个，全新未拆。想换点实用的家居小物。', 1, '深圳市', 2, '换家居日用小物', 3,
  '手办,全新,猫咪', 1, NULL, 0, 87, 9, NOW(), NOW()),
 (9004, 1, 3, 0, '宜家 木质折叠小桌', 'crmebimage/public/maintain/2023/02/23/1c0fa967eb764d918f064744cc51dc70a6f2bj3sha.jpg',
  '用了半年，桌角有一处轻微磨损，可折叠好搬运。同城自提优先。', 3, '深圳市', 1, '随便换点什么都行', 0,
  '家居,宜家,自提', 1, NULL, 0, 42, 3, NOW(), NOW()),
 (9005, 2, 9, 0, '尤克里里 入门款', 'crmebimage/public/maintain/2023/02/23/1c0fa967eb764d918f064744cc51dc70a6f2bj3sha.jpg',
  '学了三个月就吃灰了，音准正常，送备用琴弦一套。想换运动装备。', 2, '深圳市', 2, '换运动户外装备', 6,
  '乐器,尤克里里,入门', 1, NULL, 0, 63, 7, NOW(), NOW()),
 (9006, 3, 4, 0, '待审核：冬季加绒卫衣 L码', 'crmebimage/public/maintain/2023/02/23/1c0fa967eb764d918f064744cc51dc70a6f2bj3sha.jpg',
  '穿了两次，尺码买大了。换个帽子或者围巾。', 2, '深圳市', 2, '换帽子/围巾', 4,
  '服饰,卫衣', 2, NULL, 0, 0, 0, NOW(), NOW());

-- 3. 一条待处理换物申请（林小满 申请换 大粽子 的人类简史）
DELETE FROM eb_barter_apply WHERE id = 9001;
INSERT INTO eb_barter_apply (id, item_id, apply_uid, owner_uid, offer_item_ids, message, status, create_time, update_time)
VALUES (9001, 9001, 2, 1, '9002', '我用 Kindle 换你的人类简史，我这台屏幕很新，可以现场验货。', 1, NOW(), NOW());

-- 4. 一条进行中的换物订单（大粽子 与 林小满 已完成交换的示例：状态 3 待收货）
DELETE FROM eb_barter_order WHERE id = 9001;
INSERT INTO eb_barter_order (id, order_sn, apply_id, item_id, owner_uid, apply_uid, offer_item_ids, status,
                             delivery_type, owner_deliver_status, apply_deliver_status, owner_logistics_no,
                             apply_logistics_no, self_take_address, owner_confirm_recv, apply_confirm_recv,
                             owner_evaluate_id, apply_evaluate_id, create_time, update_time, complete_time)
VALUES (9001, 'BT2026102500001', 9001, 9002, 2, 1, '9001', 2, 1, 0, 0, '', '',
        '深圳市南山区科技园地铁站 A 出口', 0, 0, 0, 0, NOW(), NOW(), NULL);

-- 5. 一条举报记录
DELETE FROM eb_barter_report WHERE id = 9001;
INSERT INTO eb_barter_report (id, item_id, order_id, uid, target_uid, type, content, status, handle_result, handle_uid, create_time, update_time)
SELECT 9001, 9006, 0, 1, 3, 1, '疑似发布虚拟物品引流，描述里留了联系方式', 1, '', 0, NOW(), NOW()
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'eb_barter_report');

SELECT 'seed done' AS msg;

-- =====================================================================
-- 「我想要」演示数据（同一用户对同一物品只能想要一次：uk_item_uid 唯一索引）
-- =====================================================================
DELETE FROM eb_barter_item_like WHERE item_id BETWEEN 9001 AND 9010;
INSERT INTO eb_barter_item_like (item_id, uid, create_time) VALUES
  (9002, 1, NOW()), (9002, 3, NOW()),
  (9001, 2, NOW()), (9001, 3, NOW()),
  (9005, 1, NOW()), (9005, 2, NOW()), (9005, 3, NOW()),
  (9003, 2, NOW()),
  (9004, 3, NOW()),
  (9006, 1, NOW());
UPDATE eb_barter_item i SET want_count = (SELECT COUNT(*) FROM eb_barter_item_like l WHERE l.item_id = i.id) WHERE id BETWEEN 9001 AND 9010;

-- 演示地址（uid=1 大粽子）
DELETE FROM eb_barter_user_address WHERE uid = 1 AND id BETWEEN 9001 AND 9005;
INSERT INTO eb_barter_user_address (id, uid, name, phone, province, city, district, detail, is_default, create_time, update_time) VALUES
  (9001, 1, '大粽子', '13800138000', '广东省', '深圳市', '南山区', '科技园南区腾飞大厦 1201', 1, NOW(), NOW()),
  (9002, 1, '大粽子', '13800138000', '广东省', '深圳市', '福田区', '购物公园北园 A 座 802', 0, NOW(), NOW());

-- 演示用户城市
UPDATE eb_user SET barter_city = '深圳市' WHERE uid IN (1, 2, 3);
