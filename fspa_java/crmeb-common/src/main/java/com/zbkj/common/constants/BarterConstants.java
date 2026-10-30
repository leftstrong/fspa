package com.zbkj.common.constants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 以物换物 常量：消息类型、业务类型、快递公司
 */
public interface BarterConstants {

    /* ---------------- 站内消息类型 ---------------- */
    /** 收到新的换物申请（接收者=物品主人） */
    int MSG_APPLY_CREATED = 1;
    /** 申请被接受（接收者=申请人） */
    int MSG_APPLY_ACCEPTED = 2;
    /** 申请被拒绝（接收者=申请人） */
    int MSG_APPLY_REJECTED = 3;
    /** 申请被取消（接收者=物品主人） */
    int MSG_APPLY_CANCELLED = 4;
    /** 换物单已生成（双方） */
    int MSG_ORDER_CREATED = 5;
    /** 对方已发货（对端） */
    int MSG_ORDER_DELIVERED = 6;
    /** 对方已确认收货（对端） */
    int MSG_ORDER_CONFIRMED = 7;
    /** 换物完成（双方） */
    int MSG_ORDER_COMPLETED = 8;
    /** 有人想要我的物品（物品主人） */
    int MSG_ITEM_WANTED = 9;
    /** 系统通知 */
    int MSG_SYSTEM = 10;
    /** 收到新的换物请求（接收者=物品主人，单处于待确认） */
    int MSG_ORDER_PENDING = 11;
    /** 换物请求被接受（接收者=申请人） */
    int MSG_ORDER_ACCEPTED = 12;
    /** 换物请求被拒绝（接收者=申请人） */
    int MSG_ORDER_REJECTED = 13;
    /** 换物单被取消（对端） */
    int MSG_ORDER_CANCELLED = 14;
    /** 换物单进入纠纷（对端） */
    int MSG_ORDER_DISPUTE = 15;
    /** 换物单内新聊天消息（接收者=对端） */
    int MSG_ORDER_CHAT = 16;

    /** 消息类型 -> 文案，供列表展示与下拉筛选 */
    Map<Integer, String> MSG_TYPE_TEXT = new LinkedHashMap<Integer, String>() {
        {
            put(MSG_APPLY_CREATED, "收到换物申请");
            put(MSG_APPLY_ACCEPTED, "申请被接受");
            put(MSG_APPLY_REJECTED, "申请被拒绝");
            put(MSG_APPLY_CANCELLED, "申请被取消");
            put(MSG_ORDER_CREATED, "换物单已生成");
            put(MSG_ORDER_DELIVERED, "对方已发货");
            put(MSG_ORDER_CONFIRMED, "对方已收货");
            put(MSG_ORDER_COMPLETED, "换物完成");
            put(MSG_ITEM_WANTED, "有人想要");
            put(MSG_SYSTEM, "系统通知");
            put(MSG_ORDER_PENDING, "收到换物请求");
            put(MSG_ORDER_ACCEPTED, "请求被接受");
            put(MSG_ORDER_REJECTED, "请求被拒绝");
            put(MSG_ORDER_CANCELLED, "换物单取消");
            put(MSG_ORDER_DISPUTE, "换物纠纷");
            put(MSG_ORDER_CHAT, "换物单消息");
        }
    };

    /**
     * 消息类型归属的业务分组（用于「换物」页各条目红点）
     * incoming=收到的申请 outgoing=我发出的 order=换物单 system=系统（含「有人想要」等无需决策的告知类）
     *
     * 注：「有人想要」属于纯告知（用户无需做任何处理），与真正的系统通知同归 system，
     *     避免为它单开一个低频分组稀释红点；列表内仍用彩色类型标签区分。
     *     换物单（发货/收货/完成）是履约待办，必须独立成组，不能被公告淹没。
     */
    Map<Integer, String> MSG_GROUP = new LinkedHashMap<Integer, String>() {
        {
            put(MSG_APPLY_CREATED, "incoming");
            put(MSG_APPLY_ACCEPTED, "outgoing");
            put(MSG_APPLY_REJECTED, "outgoing");
            put(MSG_APPLY_CANCELLED, "incoming");
            put(MSG_ORDER_CREATED, "order");
            put(MSG_ORDER_DELIVERED, "order");
            put(MSG_ORDER_CONFIRMED, "order");
            put(MSG_ORDER_COMPLETED, "order");
            put(MSG_ITEM_WANTED, "system");
            put(MSG_SYSTEM, "system");
            put(MSG_ORDER_PENDING, "order");
            put(MSG_ORDER_ACCEPTED, "order");
            put(MSG_ORDER_REJECTED, "order");
            put(MSG_ORDER_CANCELLED, "order");
            put(MSG_ORDER_DISPUTE, "order");
            put(MSG_ORDER_CHAT, "order");
        }
    };

    /** 业务分组 -> 展示名（前端 tab 与后端校验共用） */
    Map<String, String> MSG_GROUP_TEXT = new LinkedHashMap<String, String>() {
        {
            put("incoming", "收到的");
            put("outgoing", "发出的");
            put("order", "换物单");
            put("system", "系统");
        }
    };

    /** 取消息类型所属分组，未命中归为 system */
    static String groupOfMsgType(int type) {
        String g = MSG_GROUP.get(type);
        return g == null ? "system" : g;
    }

    /* ---------------- 换物物品状态 ---------------- */
    /** 在售（可被发起换物 / 被「想要」） */
    int ITEM_STATUS_ON_SALE = 1;
    /** 审核中 */
    int ITEM_STATUS_AUDITING = 2;
    /** 审核驳回 */
    int ITEM_STATUS_REJECTED = 3;
    /** 已交换 / 已换出（终态，锁定不可编辑） */
    int ITEM_STATUS_EXCHANGED = 4;
    /** 交换中（换物单进行中，锁定不可编辑、不可再被发起） */
    int ITEM_STATUS_EXCHANGING = 5;
    /** 已下架 */
    int ITEM_STATUS_OFF = 6;

    /* ---------------- 智能换物（路径规划 + 只走第一棒） ---------------- */
    /**
     * 支持的最大跨越次数。
     * 跳数语义：1 跳 = 两人直接互换；2 跳 = 3 人接力；3 跳 = 4 人；5 跳 = 6 人。
     */
    int CHAIN_MAX_HOPS = 5;

    /* ---------------- 路径规划费档位（分） ----------------
     * 检索与出方案免费，只有第一棒真正成交时才收；失败、被拒、取消都不收费。
     * 定价按「实际成交方案的人数」分档，人越多平台撮合成本越高：
     *   2 人直接互换   免费（撮合最简单，鼓励双方直接换）
     *   3 人接力       10 分
     *   4-6 人接力     50 分
     */
    /** 2 人（1 跳）直接互换：免费 */
    int PATH_FEE_2P = 0;
    /** 3 人（2 跳）接力 */
    int PATH_FEE_3P = 10;
    /** 4-6 人（3-5 跳）接力 */
    int PATH_FEE_4_6P = 50;

    /** 换物成交奖励：双方各得积分（积分正向来源，让「消耗—获得」形成闭环） */
    int POINTS_PER_DEAL = 10;
    /** 新用户注册奖励 */
    int POINTS_REGISTER = 50;
    /** 首发好物奖励：审核通过后才发，每人仅一次（用 eb_user.barter_first_award 做幂等标记） */
    int POINTS_FIRST_PUBLISH = 50;

    /** 换物完成：信用分变动（正加负扣） */
    int CREDIT_PER_DEAL = 5;
    /** 互评：好评（>=4 星）加分 */
    int CREDIT_GOOD = 2;
    /** 互评：差评（<=2 星）扣分 */
    int CREDIT_BAD = 3;

    /** 按跳数取路径规划费（跳数 = 人数 − 1） */
    static int pathFee(int hops) {
        if (hops <= 1) return PATH_FEE_2P;
        if (hops == 2) return PATH_FEE_3P;
        return PATH_FEE_4_6P;
    }

    /** 兼容旧调用点：按跳数取路径规划费 */
    static int chainPoints(int hops) {
        return pathFee(hops);
    }

    /* ---------------- 快递公司 ---------------- */
    /** 发货后可自查修改单号的时长（小时）：填错单号是高频事故，给一个短窗口自行纠正 */
    int EXPRESS_EDIT_HOURS = 24;
    /** 同一侧单号最多允许修改的次数，防止反复变更让对方查不到 */
    int EXPRESS_EDIT_MAX = 3;

    /**
     * 常用快递公司（code 便于后续对接物流查询接口）
     * 新增公司只需在此追加，前端下拉自动同步
     */
    String[][] EXPRESS_LIST = {
            {"SF", "顺丰速运"},
            {"ZTO", "中通快递"},
            {"YTO", "圆通速递"},
            {"YD", "韵达快递"},
            {"STO", "申通快递"},
            {"JT", "极兔速递"},
            {"JD", "京东物流"},
            {"EMS", "EMS"},
            {"YZPY", "邮政快递包裹"},
            {"DBL", "德邦快递"},
            {"CNSD", "菜鸟速递"},
            {"SS", "闪送"},
            {"OTHER", "其他"}
    };

    /** 组装给前端的 {code,name} 列表 */
    static List<Map<String, String>> expressList() {
        List<Map<String, String>> list = new ArrayList<>();
        for (String[] e : EXPRESS_LIST) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("code", e[0]);
            m.put("name", e[1]);
            list.add(m);
        }
        return list;
    }

    /** code -> 公司名，未命中返回 code 本身 */
    static String expressName(String code) {
        if (code == null) return "";
        for (String[] e : EXPRESS_LIST) {
            if (e[0].equals(code)) return e[1];
        }
        return code;
    }

    /**
     * 公司名 / code -> 标准 code（物流查询接口需要编码）
     * 兼容历史数据：早期种子数据里存的是简称中文名（如「顺丰」「京东」），需归一成 SF / JD。
     * 三级匹配：编码精确 -> 名称精确 -> 名称互含（顺丰 ⊂ 顺丰速运）。
     */
    static String expressCode(String nameOrCode) {
        if (nameOrCode == null) return "";
        String v = nameOrCode.trim();
        if (v.isEmpty()) return "";
        for (String[] e : EXPRESS_LIST) {
            if (e[0].equalsIgnoreCase(v)) return e[0];
        }
        for (String[] e : EXPRESS_LIST) {
            if (e[1].equals(v)) return e[0];
        }
        for (String[] e : EXPRESS_LIST) {
            if (e[1].startsWith(v) || v.startsWith(e[1])) return e[0];
        }
        return v;
    }
}
