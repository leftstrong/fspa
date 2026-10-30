package com.zbkj.service.service.impl;

import com.zbkj.common.constants.BarterConstants;
import com.zbkj.service.service.BarterConfigService;
import com.zbkj.service.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 换物规则配置读取实现
 * <p>
 * 读取顺序：eb_system_config → BarterConstants 默认值。
 * 说明：不做本地缓存，避免后台改完配置后需要重启或等待过期；
 * eb_system_config 按 name 有索引，单次读取成本可忽略。
 */
@Service
public class BarterConfigServiceImpl implements BarterConfigService {

    @Autowired
    private SystemConfigService systemConfigService;

    /* ---------------- 配置键 ---------------- */

    private static final String K_POINTS_REGISTER = "barter_points_register";
    private static final String K_POINTS_FIRST_PUBLISH = "barter_points_first_publish";
    private static final String K_POINTS_PER_DEAL = "barter_points_per_deal";
    private static final String K_TOP_POINTS_PER_HOUR = "barter_top_points_per_hour";
    private static final String K_TOP_DURATION_HOUR = "barter_top_duration_hour";

    private static final String K_PATH_FEE_2P = "barter_path_fee_2p";
    private static final String K_PATH_FEE_3P = "barter_path_fee_3p";
    private static final String K_PATH_FEE_4_6P = "barter_path_fee_4_6p";
    private static final String K_CHAIN_MAX_HOPS = "barter_chain_max_hops";

    private static final String K_CREDIT_PER_DEAL = "barter_credit_per_deal";
    private static final String K_CREDIT_GOOD = "barter_credit_good";
    private static final String K_CREDIT_BAD = "barter_credit_bad";

    private static final String K_APPLY_EXPIRE_HOUR = "barter_apply_expire_hour";
    private static final String K_RECV_EXPIRE_HOUR = "barter_recv_expire_hour";
    private static final String K_EXPRESS_EDIT_HOURS = "barter_express_edit_hours";
    private static final String K_EXPRESS_EDIT_MAX = "barter_express_edit_max";

    /* ---------------- 通用读取 ---------------- */

    @Override
    public int getInt(String key, int defaultVal) {
        try {
            String v = systemConfigService.getValueByKey(key);
            if (v != null && !v.trim().isEmpty()) {
                return Integer.parseInt(v.trim());
            }
        } catch (Exception ignore) {
            // 配置不存在 / 非数字，一律回落默认值
        }
        return defaultVal;
    }

    @Override
    public String getString(String key, String defaultVal) {
        try {
            String v = systemConfigService.getValueByKey(key);
            if (v != null && !v.trim().isEmpty()) return v.trim();
        } catch (Exception ignore) {
        }
        return defaultVal;
    }

    /* ---------------- 积分规则 ---------------- */

    @Override
    public int pointsRegister() {
        return getInt(K_POINTS_REGISTER, BarterConstants.POINTS_REGISTER);
    }

    @Override
    public int pointsFirstPublish() {
        return getInt(K_POINTS_FIRST_PUBLISH, BarterConstants.POINTS_FIRST_PUBLISH);
    }

    @Override
    public int pointsPerDeal() {
        return getInt(K_POINTS_PER_DEAL, BarterConstants.POINTS_PER_DEAL);
    }

    @Override
    public int topPointsPerHour() {
        // 历史默认值存在两处（1 与 100），以 1 为准：置顶按时长计价，时长默认 24h
        return getInt(K_TOP_POINTS_PER_HOUR, 1);
    }

    @Override
    public int topDurationHour() {
        return getInt(K_TOP_DURATION_HOUR, 24);
    }

    /* ---------------- 智能换物 ---------------- */

    @Override
    public int pathFee2p() {
        return getInt(K_PATH_FEE_2P, BarterConstants.PATH_FEE_2P);
    }

    @Override
    public int pathFee3p() {
        return getInt(K_PATH_FEE_3P, BarterConstants.PATH_FEE_3P);
    }

    @Override
    public int pathFee4to6p() {
        return getInt(K_PATH_FEE_4_6P, BarterConstants.PATH_FEE_4_6P);
    }

    @Override
    public int pathFee(int hops) {
        if (hops <= 1) return pathFee2p();
        if (hops == 2) return pathFee3p();
        return pathFee4to6p();
    }

    @Override
    public int chainMaxHops() {
        return getInt(K_CHAIN_MAX_HOPS, BarterConstants.CHAIN_MAX_HOPS);
    }

    /* ---------------- 信用分 ---------------- */

    @Override
    public int creditPerDeal() {
        return getInt(K_CREDIT_PER_DEAL, BarterConstants.CREDIT_PER_DEAL);
    }

    @Override
    public int creditGood() {
        return getInt(K_CREDIT_GOOD, BarterConstants.CREDIT_GOOD);
    }

    @Override
    public int creditBad() {
        return getInt(K_CREDIT_BAD, BarterConstants.CREDIT_BAD);
    }

    /* ---------------- 时效与履约 ---------------- */

    @Override
    public int applyExpireHour() {
        return getInt(K_APPLY_EXPIRE_HOUR, 48);
    }

    @Override
    public int recvExpireHour() {
        return getInt(K_RECV_EXPIRE_HOUR, 168);
    }

    @Override
    public int expressEditHours() {
        return getInt(K_EXPRESS_EDIT_HOURS, BarterConstants.EXPRESS_EDIT_HOURS);
    }

    @Override
    public int expressEditMax() {
        return getInt(K_EXPRESS_EDIT_MAX, BarterConstants.EXPRESS_EDIT_MAX);
    }
}
