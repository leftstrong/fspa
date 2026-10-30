package com.zbkj.common.sms.provide;


import com.aliyuncs.exceptions.ClientException;
import com.zbkj.common.sms.AbstractSmsDTO;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SmsProvider {

    /** 短信供应商 */
    /** 阿里云 */
    public static final String SMS_PROVIDER_ALIYUN = "aliyun";
//    /** submail */
//    public static final String SMS_PROVIDER_SUBMAIL = "submail";

    public static Map<String, SmsProvider> smsProviderMap = new LinkedHashMap<>();

    static {
        smsProviderMap.put(SmsProvider.SMS_PROVIDER_ALIYUN, new AliyunSmsProvide());
//        smsProviderMap.put(SmsProvider.SMS_PROVIDER_SUBMAIL, new SubmailSmsProvider());
    }

    public abstract String getName();

    public abstract String getTemplateCode(String businessType);

    public abstract Map<String, String> getMsgMap(AbstractSmsDTO dto) throws Exception;

    public abstract Object send(String telephone, String templateCode, Map<String, String> params) throws ClientException;
}
