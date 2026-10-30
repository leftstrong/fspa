package com.zbkj.common.dict;

import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Chennong
 * @date 2023-05-13
 * @description 支付渠道字典
 */
public class PayChannelDict {
    public static final Map<String, Integer> DICT = new LinkedHashMap<String, Integer>();

    public static final String PAY_CHANNEL_WE_CHAT_H5_KEY = "weixinh5"; //H5唤起微信支付
    public static final int PAY_CHANNEL_WE_CHAT_H5_CODE = 2; //H5唤起微信支付
    /**
     * H5唤起微信支付
     */
    public static final PayChannelDict PAY_CHANNEL_WE_CHAT_H5 = new PayChannelDict(PAY_CHANNEL_WE_CHAT_H5_KEY, PAY_CHANNEL_WE_CHAT_H5_CODE);

    public static final String PAY_CHANNEL_WE_CHAT_PUBLIC_KEY = "public"; //公众号
    public static final int PAY_CHANNEL_WE_CHAT_PUBLIC_CODE  = 0; //公众号
    /**
     * 公众号
     */
    public static final PayChannelDict PAY_CHANNEL_WE_CHAT_PUBLIC = new PayChannelDict(PAY_CHANNEL_WE_CHAT_PUBLIC_KEY, PAY_CHANNEL_WE_CHAT_PUBLIC_CODE);

    public static final String PAY_CHANNEL_WE_CHAT_PROGRAM_KE = "routine"; //小程序
    public static final int PAY_CHANNEL_WE_CHAT_PROGRAM_CODE = 1; //小程序
    /**
     * 小程序
     */
    public static final PayChannelDict PAY_CHANNEL_WE_CHAT_PROGRAM = new PayChannelDict(PAY_CHANNEL_WE_CHAT_PROGRAM_KE, PAY_CHANNEL_WE_CHAT_PROGRAM_CODE);

    public static final String PAY_CHANNEL_WE_CHAT_APP_IOS_KEY = "weixinAppIos"; //微信App支付ios
    public static final int PAY_CHANNEL_WE_CHAT_APP_IOS_CODE = 5; //微信App支付ios
    /**
     * 微信App支付ios
     */
    public static final PayChannelDict PAY_CHANNEL_WE_CHAT_APP_IOS = new PayChannelDict(PAY_CHANNEL_WE_CHAT_APP_IOS_KEY, PAY_CHANNEL_WE_CHAT_APP_IOS_CODE);

    public static final String PAY_CHANNEL_WE_CHAT_APP_ANDROID_KEY = "weixinAppAndroid"; //微信App支付android
    public static final int PAY_CHANNEL_WE_CHAT_APP_ANDROID_CODE = 5; //微信App支付android
    /**
     * 微信App支付android
     */
    public static final PayChannelDict PAY_CHANNEL_WE_CHAT_APP_ANDROID = new PayChannelDict(PAY_CHANNEL_WE_CHAT_APP_ANDROID_KEY, PAY_CHANNEL_WE_CHAT_APP_ANDROID_CODE);

    public static final String PAY_CHANNEL_WE_CHAT_QRCODE_SCAN_KEY = "weixinQRCodeScan"; //微信扫码支付
    public static final int PAY_CHANNEL_WE_CHAT_QRCODE_SCAN_CODE = 8; //微信扫码支付
    /**
     * 微信扫码支付
     */
    public static final PayChannelDict PAY_CHANNEL_WE_CHAT_QRCODE_SCAN = new PayChannelDict(PAY_CHANNEL_WE_CHAT_QRCODE_SCAN_KEY, PAY_CHANNEL_WE_CHAT_QRCODE_SCAN_CODE);


    public static final String PAY_CHANNEL_ALI_PAY_KEY = "alipay"; //支付宝支付
    public static final int PAY_CHANNEL_ALI_PAY_CODE= 6; //支付宝支付
    public static final PayChannelDict PAY_CHANNEL_ALI_PAY = new PayChannelDict(PAY_CHANNEL_ALI_PAY_KEY, PAY_CHANNEL_ALI_PAY_CODE);


    public static final String PAY_CHANNEL_ALI_APP_PAY_KEY = "appAliPay"; //支付宝App支付
    public static final int PAY_CHANNEL_ALI_APP_PAY_CODE = 7; //支付宝App支付
    public static final PayChannelDict PAY_CHANNEL_ALI_APP_PAY = new PayChannelDict(PAY_CHANNEL_ALI_APP_PAY_KEY, PAY_CHANNEL_ALI_APP_PAY_CODE);


    private String key;
    private Integer value;

    private PayChannelDict() {
    }

    private PayChannelDict(String key, Integer value) {
        this.key = key;
        this.value = value;
        set(key, value);
    }

    public static Integer get(String key) {
        return DICT.get(key);
    }

    public static void set(String key, Integer value) {
        DICT.put(key, value);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public static String getKey(String value) {
        String key = null;
        Iterator<Map.Entry<String, Integer>> its = DICT.entrySet().iterator();
        while (its.hasNext()) {
            Map.Entry<String, Integer> entry = its.next();
            if (Objects.equals(entry.getValue(),value)) {
                key = entry.getKey();
                break;
            }
        }
        return key;
    }
}
