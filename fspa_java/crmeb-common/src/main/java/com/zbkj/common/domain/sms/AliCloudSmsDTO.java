package com.zbkj.common.domain.sms;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author Chennong
 * @date 2023-05-17
 * @description 阿里云短信参数对象
 */
@Getter
@Setter
@Accessors(chain = true)
public class AliCloudSmsDTO extends AbstractSmsDTO implements Serializable {

    private static final long serialVersionUID = -6321430360732266689L;

    /**
     * 接受者手机号
     */
    private String phoneNumbers;

    /**
     * 短信签名
     */
    private String signName;
}
