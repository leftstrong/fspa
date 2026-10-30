package com.zbkj.service.service.impl;

import cn.hutool.core.util.StrUtil;
import com.zbkj.common.model.user.UserToken;
import com.zbkj.service.service.BarterNotifyService;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.service.service.TemplateMessageService;
import com.zbkj.service.service.UserTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 以物换物 通知服务实现
 * 通过微信小程序订阅消息推送；模板 ID 由 system_config 后台可配（键名 barter_tpl_*），
 * 未配置或用户未授权则静默跳过，绝不抛异常阻断主业务。
 */
@Service("barterNotifyService")
public class BarterNotifyServiceImpl implements BarterNotifyService {

    private static final Logger logger = LoggerFactory.getLogger(BarterNotifyServiceImpl.class);

    /** UserToken.type：2 = 微信小程序 */
    private static final int WECHAT_MINI_TYPE = 2;

    @Autowired
    private TemplateMessageService templateMessageService;
    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private UserTokenService userTokenService;

    @Override
    public void notifyApplyCreated(Integer ownerUid, String itemTitle, String applicantName) {
        Map<String, String> data = new HashMap<>();
        data.put("thing1", clip(itemTitle, 20));
        data.put("thing2", clip(applicantName, 20));
        data.put("time3", "尽快查看");
        push(ownerUid, "barter_tpl_apply_created", data);
    }

    @Override
    public void notifyApplyAccepted(Integer applyUid, String itemTitle) {
        Map<String, String> data = new HashMap<>();
        data.put("thing1", clip(itemTitle, 20));
        data.put("thing2", "对方已接受，请尽快交付");
        push(applyUid, "barter_tpl_apply_accepted", data);
    }

    @Override
    public void notifyDelivered(Integer targetUid, String itemTitle, String deliverRole) {
        Map<String, String> data = new HashMap<>();
        data.put("thing1", clip(itemTitle, 20));
        data.put("thing2", "对方已发货，请确认收货");
        push(targetUid, "barter_tpl_order_delivered", data);
    }

    @Override
    public void notifyCompleted(Integer uid, String itemTitle) {
        Map<String, String> data = new HashMap<>();
        data.put("thing1", clip(itemTitle, 20));
        data.put("thing2", "换物完成，记得互评哦");
        push(uid, "barter_tpl_order_completed", data);
    }

    @Override
    public void notifyCancelled(Integer uid, String itemTitle, String reason) {
        Map<String, String> data = new HashMap<>();
        data.put("thing1", clip(itemTitle, 20));
        data.put("thing2", clip(reason, 20));
        push(uid, "barter_tpl_order_cancelled", data);
    }

    /** 统一推送：解析模板ID/OpenId，异常全吞 */
    private void push(Integer uid, String tplConfigKey, Map<String, String> data) {
        try {
            String cfg = systemConfigService.getValueByKey(tplConfigKey);
            int tplId = StrUtil.isNotBlank(cfg) ? Integer.parseInt(cfg.trim()) : 0;
            if (tplId <= 0) {
                logger.warn("barter notify skip, tpl not configured: {}", tplConfigKey);
                return;
            }
            String openId = resolveOpenId(uid);
            if (StrUtil.isBlank(openId)) {
                logger.warn("barter notify skip, openId empty uid={}", uid);
                return;
            }
            templateMessageService.pushMiniTemplateMessage(tplId, new HashMap<>(data), openId);
        } catch (Exception e) {
            logger.error("barter notify error uid={} key={} msg={}", uid, tplConfigKey, e.getMessage());
        }
    }

    private String resolveOpenId(Integer uid) {
        if (uid == null) return null;
        UserToken ut = userTokenService.getTokenByUserId(uid, WECHAT_MINI_TYPE);
        return ut != null ? ut.getToken() : null;
    }

    private String clip(String s, int max) {
        if (StrUtil.isBlank(s)) return "-";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
