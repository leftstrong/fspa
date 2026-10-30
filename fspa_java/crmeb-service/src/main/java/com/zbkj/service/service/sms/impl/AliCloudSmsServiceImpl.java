//package com.zbkj.service.service.sms.impl;
//
//import cn.hutool.json.JSONUtil;
//import com.aliyun.auth.credentials.Credential;
//import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
//import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient;
//import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest;
//import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponse;
//import com.zbkj.common.constants.SmsServiceConstants;
//import com.zbkj.common.domain.sms.AbstractSmsDTO;
//import com.zbkj.common.domain.sms.AliCloudSmsDTO;
//import com.zbkj.common.exception.CrmebException;
//import com.zbkj.service.service.sms.SmsService;
//import darabonba.core.client.ClientOverrideConfiguration;
//import org.springframework.stereotype.Service;
//
//import java.util.concurrent.CompletableFuture;
//
///**
// * @author Chennong
// * @date 2023-05-17
// * @description 阿里云短信业务
// */
//@Service(SmsServiceConstants.ALI_CLOUD_SMS_SERVICE)
//public class AliCloudSmsServiceImpl implements SmsService {
//
//    @Override
//    public String sendSms(AbstractSmsDTO smsDTO) throws CrmebException {
//        AliCloudSmsDTO aliCloudSmsDTO = (AliCloudSmsDTO) smsDTO;
//        // Configure Credentials authentication information, including ak, secret, token
//        StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
//                .accessKeyId("<your-accessKeyId>")
//                .accessKeySecret("<your-accessKeySecret>")
//                //.securityToken("<your-token>") // use STS token
//                .build());
//        // Configure the Client
//        AsyncClient client = AsyncClient.builder()
//                .region("undefined") // Region ID
//                //.httpClient(httpClient) // Use the configured HttpClient, otherwise use the default HttpClient (Apache HttpClient)
//                .credentialsProvider(provider)
//                //.serviceConfiguration(Configuration.create()) // Service-level configuration
//                // Client-level configuration rewrite, can set Endpoint, Http request parameters, etc.
//                .overrideConfiguration(
//                        ClientOverrideConfiguration.create()
//                                .setEndpointOverride("dysmsapi.aliyuncs.com")
//                        //.setConnectTimeout(Duration.ofSeconds(30))
//                )
//                .build();
//        try {
//            // Parameter settings for API request
//            SendSmsRequest sendSmsRequest = SendSmsRequest.builder()
//                    .phoneNumbers(aliCloudSmsDTO.getPhoneNumbers())
//                    .signName(aliCloudSmsDTO.getSignName())
//                    // Request-level configuration rewrite, can set Http request parameters, etc.
//                    // .requestConfiguration(RequestConfiguration.create().setHttpHeaders(new HttpHeaders()))
//                    .build();
//            // Asynchronously get the return value of the API request
//            CompletableFuture<SendSmsResponse> response = client.sendSms(sendSmsRequest);
//            // Synchronously get the return value of the API request
//            SendSmsResponse resp;
//            resp = response.get();
////        System.out.println(new Gson().toJson(resp));
//            // Asynchronous processing of return values
//        /*response.thenAccept(resp -> {
//            System.out.println(new Gson().toJson(resp));
//        }).exceptionally(throwable -> { // Handling exceptions
//            System.out.println(throwable.getMessage());
//            return null;
//        });*/
//            return JSONUtil.toJsonStr(resp);
//        } catch (Exception e) {
//            throw new CrmebException(e);
//        } finally {
//            // Finally, close the client
//            client.close();
//        }
//    }
//}
