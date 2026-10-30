package com.zbkj.service.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zbkj.common.constants.BarterConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.utils.RestTemplateUtil;
import com.zbkj.common.vo.LogisticsResultListVo;
import com.zbkj.common.vo.LogisticsResultVo;
import com.zbkj.service.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 快递鸟「即时查询」通道（新增，独立文件）。
 *
 * 存在意义：CRMEB 内置的两条通道（一号通 / 阿里云市场）都要先买服务才可用；
 * 快递鸟对中小体量是免费的（实名认证后赠送额度），因此单独接一条，作为换物单物流轨迹的数据源。
 *
 * 启用方式：把 EBusinessID 与 AppKey 配到下面任一处即可（配置齐了才会真正发起外网请求，
 * 没配时 isEnabled() 返回 false，调用方自动走原来的通道，行为与改动前一致）：
 *   1) eb_system_config 表：kdniao_ebusiness_id / kdniao_app_key
 *   2) 配置文件：kdniao.ebusiness-id / kdniao.app-key
 *
 * 签名说明：官方文档要求 DataSign = Base64(MD5(RequestData + AppKey))，但社区里长期存在
 * 两种落地写法（MD5 原始 16 字节 与 MD5 后的 32 位十六进制串）。文档示例已被 PDF 提取损坏、
 * 无法据此判断，故这里在首次调用时自动探测：命中一次即锁定该写法，后续不再试错。
 */
@Slf4j
@Service
public class KdniaoExpressClient {

    /** 快递鸟即时查询地址 */
    private static final String API_URL = "https://api.kdniao.com/Ebusiness/EbusinessOrderHandle.aspx";

    /** 即时查询指令 */
    private static final String REQUEST_TYPE = "1002";

    /** 配置键（eb_system_config） */
    private static final String CFG_EBIZ_ID = "kdniao_ebusiness_id";
    private static final String CFG_APP_KEY = "kdniao_app_key";

    /** 签名写法：0=未探测，1=Base64(MD5 原始字节)，2=Base64(MD5 十六进制串) */
    private volatile int signMode = 0;

    @Value("${kdniao.ebusiness-id:}")
    private String ymlEbusinessId;

    @Value("${kdniao.app-key:}")
    private String ymlAppKey;

    @Autowired
    private RestTemplateUtil restTemplateUtil;

    @Autowired
    private SystemConfigService systemConfigService;

    /** 是否已配置可用凭据 */
    public boolean isEnabled() {
        return StrUtil.isNotBlank(ebusinessId()) && StrUtil.isNotBlank(appKey());
    }

    /**
     * 查询物流轨迹。
     *
     * @param logisticCode 运单号
     * @param shipperCode  快递公司编码（SF/ZTO/JD…）
     * @return 轨迹；无轨迹时返回 list 为空的对象（不是异常）
     */
    public LogisticsResultVo query(String logisticCode, String shipperCode) {
        String eid = ebusinessId();
        String key = appKey();
        if (StrUtil.isBlank(logisticCode) || StrUtil.isBlank(eid) || StrUtil.isBlank(key)) {
            return null;
        }

        String reqData = buildRequestData(logisticCode, shipperCode);

        List<Integer> modes = new ArrayList<>();
        if (signMode > 0) {
            modes.add(signMode);
        } else {
            modes.add(1);
            modes.add(2);
        }

        String lastReason = null;
        for (Integer mode : modes) {
            try {
                String body = doPost(eid, key, reqData, mode);
                JSONObject json = StrUtil.isBlank(body) ? null : JSON.parseObject(body);
                if (json == null) {
                    lastReason = "快递鸟返回内容为空";
                    continue;
                }
                if (json.getBooleanValue("Success")) {
                    if (signMode == 0) {
                        signMode = mode;
                        log.info("[kdniao] 签名写法已锁定为 mode={}", mode);
                    }
                    return toVo(json, logisticCode, shipperCode);
                }
                String reason = json.getString("Reason");
                // 签名不对时才换另一种写法重试；其它失败（如单号暂无轨迹）直接当「无轨迹」返回
                if (StrUtil.isNotBlank(reason) && looksLikeSignError(reason)) {
                    lastReason = reason;
                    continue;
                }
                return toVo(json, logisticCode, shipperCode);
            } catch (Exception e) {
                lastReason = StrUtil.isBlank(e.getMessage()) ? e.getClass().getSimpleName() : e.getMessage();
                log.warn("[kdniao] 查询异常 mode={} 单号={} 原因={}", mode, logisticCode, lastReason);
            }
        }
        throw new CrmebException("快递鸟查询失败：" + (StrUtil.isBlank(lastReason) ? "未知原因" : lastReason));
    }

    /* ---------------- 内部实现 ---------------- */

    private String ebusinessId() {
        String v = ymlEbusinessId;
        if (StrUtil.isBlank(v)) {
            v = safeConfig(CFG_EBIZ_ID);
        }
        return StrUtil.isBlank(v) ? "" : v.trim();
    }

    private String appKey() {
        String v = ymlAppKey;
        if (StrUtil.isBlank(v)) {
            v = safeConfig(CFG_APP_KEY);
        }
        return StrUtil.isBlank(v) ? "" : v.trim();
    }

    private String safeConfig(String key) {
        try {
            return systemConfigService.getValueByKey(key);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildRequestData(String logisticCode, String shipperCode) {
        // 用 LinkedHashMap 语义的 JSONObject 保证字段顺序稳定，签名与发送的是同一个字符串
        JSONObject o = new JSONObject(true);
        o.put("OrderCode", "");
        o.put("ShipperCode", StrUtil.isBlank(shipperCode) ? "" : shipperCode);
        o.put("LogisticCode", logisticCode);
        return o.toJSONString();
    }

    private String doPost(String eid, String key, String reqData, int mode) throws Exception {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("RequestType", REQUEST_TYPE);
        map.add("EBusinessID", eid);
        map.add("RequestData", reqData);
        map.add("DataSign", sign(reqData, key, mode));
        map.add("DataType", "2");
        return restTemplateUtil.postFormData(API_URL, map);
    }

    private String sign(String reqData, String appKey, int mode) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest((reqData + appKey).getBytes(StandardCharsets.UTF_8));
        byte[] payload;
        if (mode == 2) {
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            payload = sb.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            payload = digest;
        }
        return Base64.getEncoder().encodeToString(payload);
    }

    private boolean looksLikeSignError(String reason) {
        String s = reason.toLowerCase();
        return s.contains("签名") || s.contains("datasign") || s.contains("sign");
    }

    private LogisticsResultVo toVo(JSONObject json, String logisticCode, String shipperCode) {
        LogisticsResultVo vo = new LogisticsResultVo();
        String no = json.getString("LogisticCode");
        vo.setNumber(StrUtil.isBlank(no) ? logisticCode : no);
        vo.setExpName(BarterConstants.expressName(shipperCode));

        String state = json.getString("State");
        vo.setDeliveryStatus(stateLabel(state));
        vo.setIsSign("3".equals(state) ? "1" : "0");

        List<LogisticsResultListVo> list = new ArrayList<>();
        JSONArray traces = json.getJSONArray("Traces");
        if (traces != null && !traces.isEmpty()) {
            // 快递鸟按时间正序给出，这里倒序成「最新在最前」，与前端把第 0 条高亮的约定一致
            for (int i = traces.size() - 1; i >= 0; i--) {
                JSONObject t = traces.getJSONObject(i);
                if (t == null) {
                    continue;
                }
                LogisticsResultListVo item = new LogisticsResultListVo();
                item.setTime(t.getString("AcceptTime"));
                item.setStatus(t.getString("AcceptStation"));
                list.add(item);
            }
        }
        vo.setList(list);
        return vo;
    }

    private String stateLabel(String state) {
        if ("2".equals(state)) {
            return "在途中";
        }
        if ("3".equals(state)) {
            return "已签收";
        }
        if ("4".equals(state)) {
            return "问题件";
        }
        return "";
    }
}
