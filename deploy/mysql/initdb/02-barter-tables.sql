-- =====================================================================
-- 良物 · 以物换物业务 建表脚本 (eb_barter_*)
-- 品牌：良物 / Slogan「物为人用，方为良物」
-- 说明：本脚本独立于原 CRMEB 电商模块，所有换物业务使用 eb_barter_* 表，
--       不复用 store / order / appointment / artisan 等原表。
-- 字符集：utf8mb4 / InnoDB（与 CRMEB 保持一致）
-- 部署注意：原 fspa 项目曾发生定制表漏建导致启动/功能异常，本脚本需随新环境一起执行。
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 1. 换物品类表（无限级，复用 CRMEB Category 思路但独立）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `eb_barter_category`;
CREATE TABLE `eb_barter_category` (
  `id`         INT(11)      NOT NULL AUTO_INCREMENT COMMENT '品类ID',
  `pid`        INT(11)      NOT NULL DEFAULT 0  COMMENT '父级ID，0为一级',
  `name`       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '品类名称',
  `icon`       VARCHAR(255) NOT NULL DEFAULT '' COMMENT '图标地址',
  `sort`       INT(11)      NOT NULL DEFAULT 0  COMMENT '排序',
  `status`     TINYINT(1)   NOT NULL DEFAULT 1  COMMENT '状态：1显示 0隐藏',
  `create_time` DATETIME    DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME    DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_pid` (`pid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='换物品类表';

-- ---------------------------------------------------------------------
-- 2. 闲置物品（好物）表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `eb_barter_item`;
CREATE TABLE `eb_barter_item` (
  `id`              INT(11)       NOT NULL AUTO_INCREMENT COMMENT '物品ID',
  `uid`             INT(11)       NOT NULL DEFAULT 0  COMMENT '发布者用户ID',
  `category_id`     INT(11)       NOT NULL DEFAULT 0  COMMENT '所属品类ID（末级）',
  `category_pid`    INT(11)       NOT NULL DEFAULT 0  COMMENT '所属一级品类ID',
  `title`           VARCHAR(128)  NOT NULL DEFAULT '' COMMENT '物品标题',
  `images`          VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '多图，逗号分隔',
  `description`     TEXT                       COMMENT '物品描述',
  `condition_level` TINYINT(1)    NOT NULL DEFAULT 1  COMMENT '成色：1全新 2九新 3八新 4七新及以下 5其他',
  `city`            VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '所在城市',
  `expect_type`     TINYINT(1)    NOT NULL DEFAULT 3  COMMENT '期望换取：1指定物品 2品类 3任意 4信用优先',
  `expect_desc`     VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '期望说明/悬赏式描述',
  `expect_category_id` INT(11)    NOT NULL DEFAULT 0  COMMENT '期望品类（expect_type=2时）',
  `tags`            VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '标签，逗号分隔',
  `status`          TINYINT(1)    NOT NULL DEFAULT 2  COMMENT '状态：0下架 1在售 2审核中 3已驳回 4已交换',
  `top_end_time`    DATETIME      DEFAULT NULL COMMENT '置顶到期时间，NULL表示未置顶',
  `top_points`      INT(11)       NOT NULL DEFAULT 0  COMMENT '本次置顶消耗积分',
  `view_count`      INT(11)       NOT NULL DEFAULT 0  COMMENT '浏览数',
  `like_count`      INT(11)       NOT NULL DEFAULT 0  COMMENT '收藏数',
  `reject_reason`   VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '驳回原因（审核驳回时填写，可追溯）',
  `create_time`     DATETIME      DEFAULT NULL COMMENT '创建时间',
  `update_time`     DATETIME      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_uid` (`uid`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status_top` (`status`, `top_end_time`),
  KEY `idx_city` (`city`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闲置物品（好物）表';

-- ---------------------------------------------------------------------
-- 3. 换物申请 / 提案表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `eb_barter_apply`;
CREATE TABLE `eb_barter_apply` (
  `id`           INT(11)      NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `item_id`      INT(11)      NOT NULL DEFAULT 0  COMMENT '被申请的物品ID',
  `apply_uid`    INT(11)      NOT NULL DEFAULT 0  COMMENT '申请人用户ID',
  `owner_uid`    INT(11)      NOT NULL DEFAULT 0  COMMENT '物品主人用户ID',
  `offer_item_ids` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '申请人提供的交换物品ID，逗号分隔（空表示纯协商）',
  `message`      VARCHAR(500) NOT NULL DEFAULT '' COMMENT '留言/协商内容',
  `status`       TINYINT(1)   NOT NULL DEFAULT 1  COMMENT '状态：0已取消 1待确认 2已接受 3已拒绝 4已生成换物单',
  `create_time`  DATETIME     DEFAULT NULL COMMENT '创建时间',
  `update_time`  DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`),
  KEY `idx_owner` (`owner_uid`),
  KEY `idx_apply` (`apply_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='换物申请/提案表';

-- ---------------------------------------------------------------------
-- 4. 换物单表（双方确认后生成）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `eb_barter_order`;
CREATE TABLE `eb_barter_order` (
  `id`                  INT(11)      NOT NULL AUTO_INCREMENT COMMENT '换物单ID',
  `order_sn`            VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '换物单号',
  `apply_id`            INT(11)      NOT NULL DEFAULT 0  COMMENT '关联申请ID',
  `item_id`             INT(11)      NOT NULL DEFAULT 0  COMMENT '主物品ID（owner方）',
  `owner_uid`           INT(11)      NOT NULL DEFAULT 0  COMMENT '物品主人用户ID',
  `apply_uid`           INT(11)      NOT NULL DEFAULT 0  COMMENT '申请人用户ID',
  `offer_item_ids`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT '交换物品ID，逗号分隔（apply方）',
  `status`              TINYINT(1)   NOT NULL DEFAULT 1  COMMENT '状态：0取消 1待交付 2待收货 3已完成 4纠纷中',
  `delivery_type`       TINYINT(1)   NOT NULL DEFAULT 1  COMMENT '交付方式：1自提 2快递',
  `owner_deliver_status` TINYINT(1)  NOT NULL DEFAULT 0  COMMENT '主人交付状态：0未交付 1已交付',
  `apply_deliver_status` TINYINT(1)  NOT NULL DEFAULT 0  COMMENT '申请人交付状态：0未交付 1已交付',
  `owner_logistics_no`  VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '主人物流单号（快递时）',
  `apply_logistics_no`  VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '申请人物流单号（快递时）',
  `self_take_address`   VARCHAR(255) NOT NULL DEFAULT '' COMMENT '自提地址',
  `owner_confirm_recv`  TINYINT(1)   NOT NULL DEFAULT 0  COMMENT '主人确认收货：0否 1是',
  `apply_confirm_recv`  TINYINT(1)   NOT NULL DEFAULT 0  COMMENT '申请人确认收货：0否 1是',
  `owner_evaluate_id`   INT(11)      NOT NULL DEFAULT 0  COMMENT '主人评价ID',
  `apply_evaluate_id`   INT(11)      NOT NULL DEFAULT 0  COMMENT '申请人评价ID',
  `create_time`         DATETIME     DEFAULT NULL COMMENT '创建时间',
  `update_time`         DATETIME     DEFAULT NULL COMMENT '更新时间',
  `complete_time`       DATETIME     DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_sn` (`order_sn`),
  KEY `idx_owner` (`owner_uid`),
  KEY `idx_apply` (`apply_uid`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='换物单表';

-- ---------------------------------------------------------------------
-- 5. 举报表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `eb_barter_report`;
CREATE TABLE `eb_barter_report` (
  `id`            INT(11)      NOT NULL AUTO_INCREMENT COMMENT '举报ID',
  `item_id`       INT(11)      NOT NULL DEFAULT 0  COMMENT '被举报物品ID（可空）',
  `order_id`      INT(11)      NOT NULL DEFAULT 0  COMMENT '被举报换物单ID（可空）',
  `uid`           INT(11)      NOT NULL DEFAULT 0  COMMENT '举报人',
  `target_uid`    INT(11)      NOT NULL DEFAULT 0  COMMENT '被举报人',
  `type`          TINYINT(1)   NOT NULL DEFAULT 1  COMMENT '类型：1违规内容 2欺诈 3其他',
  `content`       VARCHAR(500) NOT NULL DEFAULT '' COMMENT '举报内容',
  `status`        TINYINT(1)   NOT NULL DEFAULT 0  COMMENT '状态：0待处理 1已处理 2已驳回',
  `handle_result` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '处理结果',
  `handle_uid`    INT(11)      NOT NULL DEFAULT 0  COMMENT '处理人',
  `create_time`   DATETIME     DEFAULT NULL COMMENT '创建时间',
  `update_time`   DATETIME     DEFAULT NULL COMMENT '更新时间',
  `handle_time`   DATETIME     DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='换物举报表';

-- ---------------------------------------------------------------------
-- 6. 会员等级配置表（按成功交换次数自动匹配）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `eb_barter_member_level`;
CREATE TABLE `eb_barter_member_level` (
  `id`             INT(11)      NOT NULL AUTO_INCREMENT COMMENT '等级ID',
  `name`           VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '等级名称',
  `icon`           VARCHAR(255) NOT NULL DEFAULT '' COMMENT '等级徽章图标',
  `min_count`      INT(11)      NOT NULL DEFAULT 0  COMMENT '成功交换次数下限（含）',
  `max_count`      INT(11)      NOT NULL DEFAULT 0  COMMENT '成功交换次数上限（不含），0表示无上限',
  `exposure_weight` INT(11)     NOT NULL DEFAULT 0  COMMENT '信息流曝光权重，越大越靠前',
  `badge_color`    VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '徽章颜色（十六进制）',
  `sort`           INT(11)      NOT NULL DEFAULT 0  COMMENT '排序',
  `auto_match`     TINYINT(1)   NOT NULL DEFAULT 1  COMMENT '是否按次数自动匹配：1是 0否',
  `create_time`    DATETIME     DEFAULT NULL COMMENT '创建时间',
  `update_time`    DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_count` (`min_count`, `max_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='换物会员等级配置表';

-- ---------------------------------------------------------------------
-- 7. 积分日志表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `eb_barter_points_log`;
CREATE TABLE `eb_barter_points_log` (
  `id`          INT(11)      NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `uid`         INT(11)      NOT NULL DEFAULT 0  COMMENT '用户ID',
  `type`        TINYINT(1)   NOT NULL DEFAULT 1  COMMENT '类型：1获取 2消费',
  `value`       INT(11)      NOT NULL DEFAULT 0  COMMENT '变动值',
  `balance`     INT(11)      NOT NULL DEFAULT 0  COMMENT '变动后余额',
  `mark`        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '说明',
  `link_type`   TINYINT(1)   NOT NULL DEFAULT 0  COMMENT '关联类型：1置顶 2完成交换奖励 3发布奖励 4邀请',
  `link_id`     INT(11)      NOT NULL DEFAULT 0  COMMENT '关联ID',
  `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='换物积分日志表';

-- ---------------------------------------------------------------------
-- 8. 互评表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `eb_barter_evaluate`;
CREATE TABLE `eb_barter_evaluate` (
  `id`          INT(11)      NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `order_id`    INT(11)      NOT NULL DEFAULT 0  COMMENT '换物单ID',
  `uid`         INT(11)      NOT NULL DEFAULT 0  COMMENT '评价人',
  `to_uid`      INT(11)      NOT NULL DEFAULT 0  COMMENT '被评价人',
  `item_id`     INT(11)      NOT NULL DEFAULT 0  COMMENT '关联物品ID',
  `star`        TINYINT(1)   NOT NULL DEFAULT 5  COMMENT '评分 1-5',
  `content`     VARCHAR(500) NOT NULL DEFAULT '' COMMENT '评价内容',
  `images`      VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '评价图片，逗号分隔',
  `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_to_uid` (`to_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='换物互评表';

-- ---------------------------------------------------------------------
-- 9. 用户表扩展字段（eb_user 已有，执行前请先确认列不存在）
--    若为新库，可直接随 CRMEB 初始化脚本一起加列；
--    若为已上线库，请用下方 ALTER 增量添加。
-- ---------------------------------------------------------------------
-- 注意：MySQL 5.7 不支持 `ADD COLUMN IF NOT EXISTS`（那是 MariaDB 语法），
--      故此处为普通 ALTER。**不可重复执行**——若列已存在会报 1060 Duplicate column。
--      已加过列的库请跳过本段。
ALTER TABLE `eb_user`
  ADD COLUMN `barter_exchange_count` INT(11)   NOT NULL DEFAULT 0 COMMENT '成功交换次数',
  ADD COLUMN `barter_level_id`       INT(11)   NOT NULL DEFAULT 1 COMMENT '当前会员等级ID',
  ADD COLUMN `barter_points`         INT(11)   NOT NULL DEFAULT 0 COMMENT '换物积分',
  ADD COLUMN `credit_score`          INT(11)   NOT NULL DEFAULT 0 COMMENT '信用分';

-- ---------------------------------------------------------------------
-- 10. 初始会员等级数据（阈值可在管理后台调整）
--     信用分初始 = 手机绑定 +50（在绑定逻辑里加），交换成功 +5/次
-- ---------------------------------------------------------------------
INSERT INTO `eb_barter_member_level` (`name`, `icon`, `min_count`, `max_count`, `exposure_weight`, `badge_color`, `sort`, `auto_match`, `create_time`, `update_time`) VALUES
('良物新手', '/barter/level/lv1.png', 0,   3,   0,   '#9AA0A6', 5, 1, NOW(), NOW()),
('良物常客', '/barter/level/lv2.png', 3,   10,  10,  '#7FB069', 4, 1, NOW(), NOW()),
('良物达人', '/barter/level/lv3.png', 10,  30,  30,  '#4C9AFF', 3, 1, NOW(), NOW()),
('良物行家', '/barter/level/lv4.png', 30,  100, 60,  '#F4A300', 2, 1, NOW(), NOW()),
('良物传奇', '/barter/level/lv5.png', 100, 0,   100, '#C0392B', 1, 1, NOW(), NOW());

-- ---------------------------------------------------------------------
-- 11. 初始品类数据（发布页选品类用，可在管理后台「品类管理」调整）
-- ---------------------------------------------------------------------
INSERT INTO `eb_barter_category` (`pid`, `name`, `icon`, `sort`, `status`, `create_time`, `update_time`) VALUES
(0, '图书绘本', '', 1,  1, NOW(), NOW()),
(0, '数码电子', '', 2,  1, NOW(), NOW()),
(0, '家居日用', '', 3,  1, NOW(), NOW()),
(0, '服饰鞋包', '', 4,  1, NOW(), NOW()),
(0, '母婴用品', '', 5,  1, NOW(), NOW()),
(0, '运动户外', '', 6,  1, NOW(), NOW()),
(0, '玩具手办', '', 7,  1, NOW(), NOW()),
(0, '美妆个护', '', 8,  1, NOW(), NOW()),
(0, '乐器音响', '', 9,  1, NOW(), NOW()),
(0, '其他闲置', '', 10, 1, NOW(), NOW());

-- =====================================================================
-- 置顶价格「后台可配」说明：
--   复用 CRMEB 系统配置表 system_config，新增以下配置键（在管理后台「系统配置」维护）：
--   barter_top_points_per_hour  —— 每置顶 1 小时消耗的积分，默认 100
--   barter_top_duration_hour    —— 单次置顶时长（小时），默认 1
--   读取方式：SystemConfigService.getConfigValue("barter_top_points_per_hour")
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 1;
