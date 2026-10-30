package com.zbkj.service.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.common.utils.RestTemplateUtil;
import com.zbkj.common.vo.LogisticsResultListVo;
import com.zbkj.common.vo.LogisticsResultVo;
import com.zbkj.common.vo.OnePassLogisticsQueryVo;
import com.zbkj.service.service.LogisticService;
import com.zbkj.service.service.OnePassService;
import com.zbkj.service.service.SystemConfigService;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
* ExpressServiceImpl 接口实现
*  +----------------------------------------------------------------------
 *  | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 *  +----------------------------------------------------------------------
 *  | Copyright (c) 2016~2022 https://www.crmeb.com All rights reserved.
 *  +----------------------------------------------------------------------
 *  | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 *  +----------------------------------------------------------------------
 *  | Author: CRMEB Team <admin@crmeb.com>
 *  +----------------------------------------------------------------------
*/
@Data
@Service
public class LogisticsServiceImpl implements LogisticService {

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private RestTemplateUtil restTemplateUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private OnePassService onePassService;

    private String redisKey = Constants.LOGISTICS_KEY;
    private Long redisCacheSeconds = 1800L;

    private String expressNo;


    /** 快递
     * @param expressNo String 物流单号
     * @param type String 快递公司字母简写：不知道可不填 95%能自动识别，填写查询速度会更快 https://market.aliyun.com/products/56928004/cmapi021863.html#sku=yuncode15863000015
     * @param com 快递公司编号
     * @author Mr.Zhang
     * @since 2020-06-10
     * @return Express
     */
    @Override
    public LogisticsResultVo info(String expressNo, String type, String com, String phone) {
        LogisticsResultVo resultVo = new LogisticsResultVo();
        setExpressNo(expressNo);
        JSONObject result = getCache();
        if (ObjectUtil.isNotNull(result)) {
            return JSONObject.toJavaObject(result, LogisticsResultVo.class);
        }
        String logisticsType = systemConfigService.getValueByKeyException("logistics_type");
        if (logisticsType.equals("1")) {// 平台查询
            OnePassLogisticsQueryVo queryVo = onePassService.exprQuery(expressNo, com);
            if (ObjectUtil.isNull(queryVo)) {
                return resultVo;
            }
            // 一号通vo转公共返回vo
            resultVo = queryToResultVo(queryVo);
            String jsonString = JSONObject.toJSONString(resultVo);
            saveCache(JSONObject.parseObject(jsonString));
        }
        if ("2".equals(logisticsType)) {// 阿里云市场查询
            String appCode = systemConfigService.getValueByKey(Constants.CONFIG_KEY_LOGISTICS_APP_CODE);
            // 占位 key（未在后台替换为真实 APPCODE）直接短路，避免拿假凭据去请求网关被记无效调用
            if (StrUtil.isBlank(appCode)
                    || appCode.startsWith("MOCK")
                    || appCode.contains("REPLACE_ME")) {
                throw new CrmebException("物流查询服务未开通：请在后台「物流设置」填写阿里云市场 APPCODE");
            }

            // 接口地址支持按服务商配置（阿里云市场不同商品网关地址不同），未配置时用 CRMEB 默认地址
            String apiUrl = systemConfigService.getValueByKey(Constants.CONFIG_KEY_LOGISTICS_API_URL);
            if (StrUtil.isBlank(apiUrl)) {
                apiUrl = Constants.LOGISTICS_API_URL;
            }

            // 顺丰请输入单号 : 收件人或寄件人手机号后四位。例如：123456789:1234
            // 调用方可能直接给后四位（换物单从地址里提取），也可能给完整 11 位手机号，两种都要兼容，
            // 否则 4 位号码再被 sub(7,-1) 截一次就成了空串，顺丰永远查不到轨迹
            if (isShunfeng(com) && StrUtil.isNotBlank(phone) && !expressNo.contains(":")) {
                String p = phone.trim();
                String tail = "";
                if (p.length() == 4) {
                    tail = p;
                } else if (p.length() >= 11) {
                    tail = StrUtil.sub(p, p.length() - 4, -1);
                }
                if (StrUtil.isNotBlank(tail)) {
                    expressNo = expressNo.concat(":").concat(tail);
                }
            }
            String url = apiUrl + "?no=" + expressNo;
            if (StringUtils.isNotBlank(type)) {
                url += "&type=" + type;
            }

            HashMap<String, String> header = new HashMap<>();
            header.put("Authorization", "APPCODE " + appCode);

            JSONObject data;
            try {
                data = restTemplateUtil.getData(url, header);
            } catch (HttpClientErrorException e) {
                // 阿里云市场网关鉴权失败时返回 401 且 body 为空，必须按状态码给出可读原因
                int code = e.getRawStatusCode();
                if (code == 401 || code == 403) {
                    throw new CrmebException("物流查询服务未开通：APPCODE 无效，或套餐未开通/查询次数已用完");
                }
                if (code == 429) {
                    throw new CrmebException("物流查询失败：调用频率超限，请稍后再试");
                }
                throw new CrmebException("物流查询失败：接口返回 HTTP " + code);
            } catch (Exception e) {
                throw new CrmebException("物流查询失败：网络异常或接口不可达，请稍后重试");
            }
            checkResult(data);
            //把数据解析成对象返回到前端
            result = data.getJSONObject("result");
            if (ObjectUtil.isNull(result)) {
                return resultVo;
            }
            saveCache(result);
            resultVo = JSONObject.toJavaObject(result, LogisticsResultVo.class);
        }
        return resultVo;
    }

    /**
     * 是否顺丰系快递（顺丰查询需要拼「单号:手机号后四位」，否则接口查不到轨迹）
     */
    private boolean isShunfeng(String com) {
        if (StrUtil.isBlank(com)) {
            return false;
        }
        String c = com.trim().toUpperCase();
        return c.equals("SF") || c.equals("SHUNFENG")
                || c.equals("SHUNFENGKUAIYUN") || c.equals("SFEXPRESS");
    }

    /**
     * 一号通vo转公共返回vo
     */
    private LogisticsResultVo queryToResultVo(OnePassLogisticsQueryVo queryVo) {
        LogisticsResultVo resultVo = new LogisticsResultVo();
        resultVo.setNumber(queryVo.getNum());
        resultVo.setExpName(queryVo.getCom());
        resultVo.setIsSign(queryVo.getIscheck());
        resultVo.setDeliveryStatus(queryVo.getStatus());

        if (CollUtil.isNotEmpty(queryVo.getContent())) {
            List<LogisticsResultListVo> list = CollUtil.newArrayList();
            queryVo.getContent().forEach(i -> {
                LogisticsResultListVo listVo = new LogisticsResultListVo();
                listVo.setTime(i.getTime());
                listVo.setStatus(i.getStatus());
                list.add(listVo);
            });
            resultVo.setList(list);
        }
        return resultVo;
    }

    /** 获取快递缓存
     * @author Mr.Zhang
     * @since 2020-07-06
     * @return JSONObject
     */
    private JSONObject getCache() {
        Object data = redisUtil.get(getRedisKey() + getExpressNo());
        if(null != data){
         return JSONObject.parseObject(data.toString());
        }
        return null;
    }

    /** 获取快递缓存
     * @param data JSONObject 需要保存的数据
     * @author Mr.Zhang
     * @since 2020-07-06
     */
    private void saveCache(JSONObject data) {
        redisUtil.set(getRedisKey() + getExpressNo(), data.toJSONString(), getRedisCacheSeconds(), TimeUnit.SECONDS);
    }

    /** 获取快递缓存
     * @param data JSONObject 检测返回的结果
     * @author Mr.Zhang
     * @since 2020-07-06
     */
    private void checkResult(JSONObject data) {
        if (ObjectUtil.isNull(data)) {
            throw new CrmebException("物流查询失败：接口无响应，请稍后重试");
        }
        String status = data.getString("status");
        String msg = data.getString("msg");
        if (StrUtil.isBlank(msg)) {
            // 阿里云市场网关错误有时用 message 字段
            msg = data.getString("message");
        }
        // 阿里云市场网关类错误（401/403/429 等）不一定返回 status 字段，单独兜底
        if (StrUtil.isBlank(status)) {
            throw new CrmebException("物流查询失败：" + (StrUtil.isBlank(msg)
                    ? "请检查 APPCODE 是否正确、套餐是否已开通或次数是否用完" : msg));
        }
        if (!"0".equals(status)) {
            throw new CrmebException(StrUtil.isBlank(msg) ? "物流查询失败" : msg);
        }
    }
}

