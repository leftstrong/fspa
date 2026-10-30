//package com.zbkj.service.service.sms;
//
//import com.aliyun.oss.ServiceException;
//import com.zbkj.common.dict.SmsServiceDict;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//
///**
// * @author Chennong
// * @date 2023-05-17
// * @description 短信服务供应器
// */
//@Component
//public class SmsProvider {
//
//    @Autowired
//    private Map<String, SmsService> serviceMap;
//
//    /**
//     * 根据类型获取短信服务
//     *
//     * @param smsType ---SmsServiceConstants
//     * @return
//     * @throws ServiceException
//     */
//    public SmsService provide(String smsType) throws ServiceException {
//        return serviceMap.get(SmsServiceDict.getSmsServiceName(smsType));
//    }
//}
