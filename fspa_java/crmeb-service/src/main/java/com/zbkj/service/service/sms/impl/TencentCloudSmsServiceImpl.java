//package com.zbkj.service.service.sms.impl;
//
//import com.tencentcloudapi.common.exception.TencentCloudSDKException;
//import com.tencentcloudapi.sms.v20210111.SmsClient;
//import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
//import com.zbkj.common.constants.SmsServiceConstants;
//import com.zbkj.common.domain.sms.AbstractSmsDTO;
//import com.zbkj.common.domain.sms.TencentCloudSmsDTO;
//import com.zbkj.common.exception.CrmebException;
//import com.zbkj.service.service.sms.SmsService;
//import org.springframework.stereotype.Service;
//
//import java.util.Optional;
//
///**
// * @author Chennong
// * @date 2023-05-17
// * @description 阿里云短信业务
// */
//@Service(SmsServiceConstants.TENCENT_CLOUD_SMS_SERVICE)
//public class TencentCloudSmsServiceImpl implements SmsService {
//
//    @Override
//    public String sendSms(AbstractSmsDTO smsDTO) throws CrmebException {
//        TencentCloudSmsDTO tencentCloudSmsDTO = (TencentCloudSmsDTO) smsDTO;
//        SmsClient smsClient = new SmsClient(new com.tencentcloudapi.common.Credential(), "");
//        com.tencentcloudapi.sms.v20210111.models.SendSmsRequest sendSmsRequest = new com.tencentcloudapi.sms.v20210111.models.SendSmsRequest();
//        /* 短信应用ID: 短信SdkAppId在 [短信控制台] 添加应用后生成的实际SdkAppId，示例如1400006666 */
//        sendSmsRequest.setSmsSdkAppId("");
//        /* 短信签名内容: 使用 UTF-8 编码，必须填写已审核通过的签名，签名信息可登录 [短信控制台] 查看 */
//        sendSmsRequest.setSignName(tencentCloudSmsDTO.getSignName());
//        /* 国际/港澳台短信 SenderId: 国内短信填空，默认未开通，如需开通请联系 [sms helper] */
//        sendSmsRequest.setSenderId("");
//        /* 下发手机号码，采用 E.164 标准，+[国家或地区码][手机号]
//         * 示例如：+8613711112222， 其中前面有一个+号 ，86为国家码，13711112222为手机号，最多不要超过200个手机号 */
//        sendSmsRequest.setPhoneNumberSet(tencentCloudSmsDTO.getPhoneNumbers());
//        /* 短信号码扩展号: 默认未开通，如需开通请联系 [sms helper] */
//        sendSmsRequest.setExtendCode("");
//        /* 用户的 session 内容: 可以携带用户侧 ID 等上下文信息，server 会原样返回 */
//        sendSmsRequest.setSessionContext("");
//        /* 模板 ID: 必须填写已审核通过的模板 ID。模板ID可登录 [短信控制台] 查看 */
//        sendSmsRequest.setTemplateId("");
//        /* 模板参数: 若无模板参数，则设置为空 */
//        sendSmsRequest.setTemplateParamSet(null);
//        com.tencentcloudapi.sms.v20210111.models.SendSmsResponse sendSmsResponse;
//        try {
//            sendSmsResponse = smsClient.SendSms(sendSmsRequest);
//        } catch (TencentCloudSDKException e) {
//            throw new CrmebException(e);
//        }
//        return Optional.ofNullable(sendSmsResponse).orElse(new SendSmsResponse()).toString();
//    }
//}
