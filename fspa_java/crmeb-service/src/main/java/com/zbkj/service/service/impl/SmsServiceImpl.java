package com.zbkj.service.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.aliyuncs.exceptions.ClientException;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.SmsApplyTempRequest;
import com.zbkj.common.request.SmsModifySignRequest;
import com.zbkj.common.sms.AbstractSmsDTO;
import com.zbkj.common.sms.dto.*;
import com.zbkj.common.sms.provide.SmsProvider;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.common.utils.ServerUtil;
import com.zbkj.common.utils.ValidateFormUtil;
import com.zbkj.common.vo.MyRecord;
import com.zbkj.service.service.SmsService;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.service.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * SmsServiceImpl 接口实现
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2022 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 */
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private UserService userService;

    /**
     * 修改签名
     */
    @Override
    public Boolean modifySign(SmsModifySignRequest request) {
//        ValidateFormUtil.isPhoneException(request.getPhone());
//        String token = onePassUtil.getToken();
//        HashMap<String, String> header = onePassUtil.getCommonHeader(token);
//
//        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
//        map.add("sign", request.getSign());
//        map.add("phone", request.getPhone());
//        map.add("verify_code", request.getCode());
//
//        onePassUtil.postFrom(OnePassConstants.ONE_PASS_API_URL + OnePassConstants.ONE_PASS_SMS_MODIFY_URI, map, header);
        return Boolean.TRUE;
    }

    @Override
    public MyRecord temps(PageParamRequest pageParamRequest) {
        return null;
    }


    /**
     * 申请模板消息
     */
    @Override
    public Boolean applyTempMessage(SmsApplyTempRequest request) {
//        String token = onePassUtil.getToken();
//        HashMap<String, String> header = onePassUtil.getCommonHeader(token);
//        MultiValueMap<String, Object> param = new LinkedMultiValueMap<>();
//        param.add("title", request.getTitle());
//        param.add("content", request.getContent());
//        param.add("type", request.getType());
//
//        onePassUtil.postFrom(OnePassConstants.ONE_PASS_API_URL + OnePassConstants.ONE_PASS_TEMP_APPLY_URI, param, header);
        return Boolean.TRUE;
    }

    /**
     * 模板申请记录
     *
     * @param type (1=验证码 2=通知 3=推广)
     */
    @Override
    public MyRecord applys(Integer type, PageParamRequest pageParamRequest) {
//        String token = onePassUtil.getToken();
//        HashMap<String, String> header = onePassUtil.getCommonHeader(token);
//        MultiValueMap<String, Object> param = new LinkedMultiValueMap<>();
//        param.add("page", pageParamRequest.getPage());
//        param.add("limit", pageParamRequest.getLimit());
//        if (ObjectUtil.isNotNull(type)) {
//            param.add("temp_type", type);
//        }
//
//        JSONObject post = onePassUtil.postFrom(OnePassConstants.ONE_PASS_API_URL + OnePassConstants.ONE_PASS_APPLYS_LIST_URI, param, header);
//        JSONObject jsonObject = post.getJSONObject("data");
//        JSONArray jsonArray = jsonObject.getJSONArray("data");
//        MyRecord myRecord = new MyRecord();
//        if (CollUtil.isEmpty(jsonArray)) {
//            return myRecord.set("count", 0);
//        }
//        List<MyRecord> recordList = jsonArray.stream().map(i -> {
//            MyRecord record = new MyRecord();
//            record.setColums((JSONObject) i);
//            switch (record.getInt("temp_type")) {
//                case 1:
//                    record.set("type", "验证码");
//                    break;
//                case 2:
//                    record.set("type", "通知");
//                    break;
//                case 3:
//                    record.set("type", "营销短信");
//                    break;
//            }
//            return record;
//        }).collect(Collectors.toList());
//
//        myRecord.set("count", recordList.size());
//        myRecord.set("data", recordList);
        return null;
    }

    /**
     * 发送公共验证码
     *
     * @param phone 手机号
     * @return Boolean
     * 1.校验后台是否配置一号通
     * 2.一号通是否剩余短信条数
     * 3.发送短信
     */
    @Override
    public Boolean sendCommonCode(String phone) {
        try {
            ValidateFormUtil.isPhone(phone, "手机号码错误");
            //获取短信验证码过期时间
            String codeExpireStr = systemConfigService.getValueByKey(Constants.CONFIG_KEY_SMS_CODE_EXPIRE);
            if (StrUtil.isBlank(codeExpireStr) || Integer.parseInt(codeExpireStr) == 0) {
                codeExpireStr = Constants.NUM_FIVE + "";// 默认5分钟过期
            }
            String msgCode = ServerUtil.getRandomNumbers();
            sendSms(new VerifyCodeSmsDTO().setCode(msgCode).setTelephone(phone));
            // 将验证码存入redis
            redisUtil.set(userService.getValidateCodeRedisKey(phone), msgCode, Long.valueOf(codeExpireStr), TimeUnit.MINUTES);
            return true;
        } catch (Exception e) {
            throw new CrmebException("发送短信失败，请联系后台管理员");
        }
    }

    /**
     * 发送支付成功短信
     *
     * @param phone     手机号
     * @param orderNo   订单编号
     * @param payPrice  支付金额
     * @param msgTempId 短信模板id
     * @return Boolean
     */
    @Override
    public Boolean sendPaySuccess(String phone, String orderNo, BigDecimal payPrice, Integer msgTempId) {
        try {
            ValidateFormUtil.isPhone(phone, "手机号码错误");
            sendSms(new PaySuccessSmsDTO().setOrder_id(orderNo).setPay_price(payPrice.toString()).setTelephone(phone));
            return true;
        } catch (Exception e) {
            throw new CrmebException("发送短信失败，请联系后台管理员");
        }
    }

    /**
     * 发送管理员下单短信提醒
     *
     * @param phone     手机号
     * @param orderNo   订单编号
     * @param realName  管理员名称
     * @param msgTempId 短信模板id
     * @return Boolean
     */
    @Override
    public Boolean sendCreateOrderNotice(String phone, String orderNo, String realName, Integer msgTempId) {
        try {
            ValidateFormUtil.isPhone(phone, "手机号码错误");
            sendSms(new CreateOrderNoticeSmsDTO().setOrder_id(orderNo).setAdmin_name(realName).setTelephone(phone));
            return true;
        } catch (Exception e) {
            throw new CrmebException("发送短信失败，请联系后台管理员");
        }
    }

    /**
     * 发送订单支付成功管理员提醒短信
     *
     * @param phone     手机号
     * @param orderNo   订单编号
     * @param realName  管理员名称
     * @param msgTempId 短信模板id
     * @return Boolean
     */
    @Override
    public Boolean sendOrderPaySuccessNotice(String phone, String orderNo, String realName, Integer msgTempId) {
        try {
            ValidateFormUtil.isPhone(phone, "手机号码错误");
            sendSms(new OrderPaySuccessNoticeSmsDTO().setOrder_id(orderNo).setAdmin_name(realName).setTelephone(phone));
            return true;
        } catch (Exception e) {
            throw new CrmebException("发送短信失败，请联系后台管理员");
        }
    }

    /**
     * 发送用户退款管理员提醒短信
     *
     * @param phone     手机号
     * @param orderNo   订单编号
     * @param realName  管理员名称
     * @param msgTempId 短信模板id
     * @return Boolean
     */
    @Override
    public Boolean sendOrderRefundApplyNotice(String phone, String orderNo, String realName, Integer msgTempId) {
        try {
            ValidateFormUtil.isPhone(phone, "手机号码错误");
            sendSms(new OrderRefundApplyNoticeSmsDTO().setOrder_id(orderNo).setAdmin_name(realName).setTelephone(phone));
            return true;
        } catch (Exception e) {
            throw new CrmebException("发送短信失败，请联系后台管理员");
        }
    }

    /**
     * 发送用户确认收货管理员提醒短信
     *
     * @param phone     手机号
     * @param orderNo   订单编号
     * @param realName  管理员名称
     * @param msgTempId 短信模板id
     */
    @Override
    public Boolean sendOrderReceiptNotice(String phone, String orderNo, String realName, Integer msgTempId) {
        try {
            ValidateFormUtil.isPhone(phone, "手机号码错误");
            sendSms(new OrderReceiptNoticeSmsDTO().setOrder_id(orderNo).setAdmin_name(realName).setTelephone(phone));
            return true;
        } catch (Exception e) {
            throw new CrmebException("发送短信失败，请联系后台管理员");
        }
    }

    /**
     * 发送订单改价提醒短信
     *
     * @param phone     手机号
     * @param orderNo   订单编号
     * @param price     修改后的支付金额
     * @param msgTempId 短信模板id
     * @return Boolean
     */
    @Override
    public Boolean sendOrderEditPriceNotice(String phone, String orderNo, BigDecimal price, Integer msgTempId) {
        try {
            ValidateFormUtil.isPhone(phone, "手机号码错误");
            sendSms(new OrderEditPriceNoticeSmsDTO().setOrder_id(orderNo).setPay_price(price.toString()).setTelephone(phone));
            return true;
        } catch (Exception e) {
            throw new CrmebException("发送短信失败，请联系后台管理员");
        }
    }

    /**
     * 发送订单发货提醒短信
     *
     * @param phone     手机号
     * @param nickName  用户昵称
     * @param storeName 商品名称
     * @param orderNo   订单编号
     * @param msgTempId 短信模板id
     */
    @Override
    public Boolean sendOrderDeliverNotice(String phone, String nickName, String storeName, String orderNo, Integer msgTempId) {
        try {
            ValidateFormUtil.isPhone(phone, "手机号码错误");
            sendSms(new OrderDeliverNoticeSmsDTO().setOrder_id(orderNo).setNickname(nickName).setStore_name(storeName).setTelephone(phone));
            return true;
        } catch (Exception e) {
            throw new CrmebException("发送短信失败，请联系后台管理员");
        }
    }

    /**
     * 短信发送
     * @param dto
     */
    private void sendSms(AbstractSmsDTO dto) {
        Object data = null;
        try {
            SmsProvider provider = SmsProvider.smsProviderMap.get(SmsProvider.SMS_PROVIDER_ALIYUN);
            String templateCode = provider.getTemplateCode(dto.getBusinessType());
            if (StringUtils.isEmpty(templateCode)) {
                log.error("当前短信运用上不存在该业务的短信模板, 短信供应商：[{}]， 业务类型：[{}]", provider.getName(), dto.getBusinessType());
                return;
            }
            Map<String, String> map = provider.getMsgMap(dto);
            data = provider.send(dto.getTelephone(), templateCode, map);
            log.info("aliyunSms response: {}", data.toString());
        }catch (ClientException clientEx){
            log.error("aliyunSms ClientException:Exception: {}" ,clientEx);
            log.error("aliyunSms ClientException:data: {}" ,data == null ? "data returns null" : JSON.toJSONString(data));
        } catch (Exception e) {
            log.error("短信发送异常 e: {}", e);
        }
    }
}
