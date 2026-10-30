package com.zbkj.service.service;

/**
 * 换物规则配置读取服务
 * <p>
 * 所有「可运营调整」的数值（积分、路径费、信用分、超时、履约窗口）统一从这里取，
 * 底层读 eb_system_config（键名前缀 barter_）。未配置、配置为空或格式非法时，
 * 一律回落到 BarterConstants 中的内置默认值，保证线上永不因配置缺失而报错。
 * <p>
 * 与直接读常量的区别：常量的角色从「唯一真相」降级为「默认值兜底」。
 */
public interface BarterConfigService {

    /* ---------------- 通用读取 ---------------- */

    /** 读整数配置，缺失或非法时返回 defaultVal */
    int getInt(String key, int defaultVal);

    /** 读字符串配置，缺失时返回 defaultVal */
    String getString(String key, String defaultVal);

    /* ---------------- 积分规则 ---------------- */

    /** 新用户注册奖励积分 */
    int pointsRegister();

    /** 首发好物奖励积分（审核通过后发放，每人一次） */
    int pointsFirstPublish();

    /** 换物成交奖励积分（双方各得） */
    int pointsPerDeal();

    /** 置顶单价（积分/小时） */
    int topPointsPerHour();

    /** 置顶时长（小时） */
    int topDurationHour();

    /* ---------------- 智能换物 ---------------- */

    /** 路径规划费：2 人直接互换 */
    int pathFee2p();

    /** 路径规划费：3 人接力（2 跳） */
    int pathFee3p();

    /** 路径规划费：4-6 人接力（3-5 跳） */
    int pathFee4to6p();

    /** 按跳数取路径规划费（跳数 = 人数 − 1） */
    int pathFee(int hops);

    /** 智能换物支持的最大跳数 */
    int chainMaxHops();

    /* ---------------- 信用分 ---------------- */

    /** 换物成交：信用分增加 */
    int creditPerDeal();

    /** 互评好评（>=4 星）：信用分增加 */
    int creditGood();

    /** 互评差评（<=2 星）：信用分扣减（正数表示扣多少） */
    int creditBad();

    /* ---------------- 时效与履约 ---------------- */

    /** 换物申请超时关闭（小时） */
    int applyExpireHour();

    /** 发货后超时自动确认收货（小时） */
    int recvExpireHour();

    /** 发货后可自行改单号的窗口（小时） */
    int expressEditHours();

    /** 同一侧最多允许改单号的次数 */
    int expressEditMax();
}
