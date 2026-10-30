package com.zbkj.common.dict;

import com.aliyun.oss.ServiceException;
import com.zbkj.common.constants.SmsServiceConstants;
import com.zbkj.common.exception.CrmebException;

/**
 * @author Chennong
 * @date 2023-05-17
 * @description 短信服务名称字典
 */
public enum SmsServiceDict {

    /**
     * 阿里云短信
     */
    ALI_CLOUD {
        @Override
        public String smsType() {
            return SmsServiceConstants.ALI_CLOUD;
        }

        @Override
        public String serviceName() {
            return SmsServiceConstants.ALI_CLOUD_SMS_SERVICE;
        }
    },

    /**
     * 腾讯云短信
     */
    TENCENT_CLOUD {
        @Override
        public String smsType() {
            return SmsServiceConstants.TENCENT_CLOUD;
        }

        @Override
        public String serviceName() {
            return SmsServiceConstants.TENCENT_CLOUD_SMS_SERVICE;
        }
    };

    /**
     * 短信类别
     *
     * @return
     */
    abstract public String smsType();

    /**
     * 短信服务名
     *
     * @return
     */
    abstract public String serviceName();

    public static String getSmsServiceName(String smsType) throws ServiceException {
        for (SmsServiceDict smsServiceDict : SmsServiceDict.values()) {
            if (smsServiceDict.smsType().equals(smsType)) {
                return smsServiceDict.serviceName();
            }
        }
        throw new CrmebException("没有该短信服务");
    }
}


