package com.zbkj.common.sms.templatecode;

import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云短信模板类型
 * 返回值为 "" 表示该业务未提供模板
 */
public enum AliyunSmsTemplateCode {

    /**
     *【FCKJ】您的验证码为${code}（客服绝不会索取此验证码，切勿告知他人），请在页面中输入以完成验证！
     */
    verifyCode {
        @Override
        public String getCode() {
            return "SMS_461345168";
        }
    },
    /**
     * 【FCKJ】您购买的商品已支付成功，支付金额{$pay_price}元，订单号{$order_id},感谢您的光临！
     */
    paySuccess {
        @Override
        public String getCode() {
            return "SMS_461425159";
        }
    },

    /**
     * FCKJ】{$admin_name}管理员,您有一笔退款订单待处理，订单号{$order_id}!
     */
    adminApplyOrderRefund{
        @Override
        public String getCode() {
            return "SMS_461350162";
        }
    },

    /**
     * 【FCKJ】{$admin_name}管理员,您有一笔订单已经确认收货，订单号{$order_id}!
     */
    adminReceiptGoods{
        @Override
        public String getCode() {
            return "SMS_461300161";
        }
    },

    /**
     * 【FCKJ】{$admin_name}管理员,您有一笔支付成功的订单待处理，订单号{$order_id}!
     */
    adminPaySuccess{
        @Override
        public String getCode() {
            return "SMS_461300162";
        }
    },

    /**
     * 【FCKJ】您的订单{$order_id}，实际支付金额已被修改为{$pay_price}。
     */
    modifyOrderPrice{
        @Override
        public String getCode() {
            return "SMS_461310166";
        }
    },

    /**
     * 【FCKJ】亲爱的用户{$nickname}您的商品{$store_name}，订单号{$order_id}已发货，请注意查收！
     */
    deliverGoods{
        @Override
        public String getCode() {
            return "SMS_461385182";
        }
    },

    /**
     * 【FCKJ】{$admin_name}管理员，您有一笔已支付的订单待处理，订单号为{$order_id}！
     */
    adminPlaceAnOrder{
        @Override
        public String getCode() {
            return "SMS_461350163";
        }
    },

    /**
     * 【FCKJ】亲爱的{$real_name}，您有一个新的预约单，时间为{$appointment_date}，请知晓！
     */
    artisanNotice{
        @Override
        public String getCode() {
            return "SMS_461560231";
        }
    };

    abstract public String getCode();

    public static Map<String, AliyunSmsTemplateCode> businessTypeMap = new HashMap<>();

    static {
        AliyunSmsTemplateCode[] businessType = AliyunSmsTemplateCode.values();
        for (AliyunSmsTemplateCode type : businessType) {
            businessTypeMap.put(type.name(), type);
        }
    }
}
