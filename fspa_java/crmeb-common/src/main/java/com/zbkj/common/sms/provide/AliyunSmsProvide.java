package com.zbkj.common.sms.provide;

import com.alibaba.fastjson.JSON;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.zbkj.common.sms.AbstractSmsDTO;
import com.zbkj.common.sms.config.AliyunConfig;
import com.zbkj.common.sms.templatecode.AliyunSmsTemplateCode;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collections;
import java.util.Map;

public class AliyunSmsProvide extends SmsProvider {
    @Override
    public String getName() {
        return SmsProvider.SMS_PROVIDER_ALIYUN;
    }

    @Override
    public String getTemplateCode(String businessType) {
        return AliyunSmsTemplateCode.businessTypeMap.get(businessType).getCode();
    }

    @Override
    public Map<String, String> getMsgMap(AbstractSmsDTO dto) throws Exception {
        return dto.convertToMap();
    }

    @Override
    public Object send(String telephone, String templateCode, Map<String,String> params) throws ClientException {
        DefaultProfile profile = DefaultProfile.getProfile(AliyunConfig.Region, AliyunConfig.AccessKey, AliyunConfig.AccessKeySecret);
        IAcsClient client = new DefaultAcsClient(profile);
        CommonRequest request = new CommonRequest();
        request.setMethod(MethodType.POST);
        request.setDomain(AliyunConfig.Domain);
        request.setVersion(AliyunConfig.Version);
        request.setAction("SendSms");
        request.putQueryParameter("RegionId", AliyunConfig.Region);
        request.putQueryParameter("PhoneNumbers", telephone);
        request.putQueryParameter("SignName", AliyunConfig.SignName);
        request.putQueryParameter("TemplateCode", templateCode);
        if (!CollectionUtils.isEmpty(Collections.singleton(params))) {
            request.putQueryParameter("TemplateParam", JSON.toJSONString(params));
        }
        CommonResponse response = client.getCommonResponse(request);
        return JSON.toJSONString(response.getData());
    }
}
