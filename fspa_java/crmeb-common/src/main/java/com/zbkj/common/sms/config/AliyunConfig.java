package com.zbkj.common.sms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云短信配置
 */
@Configuration
public class AliyunConfig {

    public static String AccessKey;
    public static String AccessKeySecret;
    public static String Region;
    public static String Version;
    public static String SignName;
    public static String Domain;


    @Value("${aliyun.AccessKey}")
    public void setAccessKey(String accessKey) {
        AccessKey = accessKey;
    }
    @Value("${aliyun.AccessKeySecret}")
    public void setAccessKeySecret(String accessKeySecret) {
        AccessKeySecret = accessKeySecret;
    }
    @Value("${aliyun.Region}")
    public void setRegion(String region) {
        Region = region;
    }
    @Value("${aliyun.Version}")
    public void setVersion(String version) {
        Version = version;
    }
    @Value("${aliyun.SignName}")
    public void setSignName(String signName) {
        SignName = signName;
    }
    @Value("${aliyun.Domain}")
    public void setDomain(String domain) {
        Domain = domain;
    }
}
