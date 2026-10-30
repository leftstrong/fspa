package com.zbkj.common.domain.sms;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author Chennong
 * @date 2023-05-17
 * @description 腾讯云短信参数对象
 */
@Getter
@Setter
@Accessors(chain = true)
public class TencentCloudSmsDTO extends AbstractSmsDTO implements Serializable {

    private static final long serialVersionUID = -739727598967361999L;

    /**
     * 接受者手机号
     */
    private String[] phoneNumbers;

    /**
     * 短信签名
     */
    private String signName;
}
