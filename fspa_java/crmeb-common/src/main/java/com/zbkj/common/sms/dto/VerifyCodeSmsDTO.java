package com.zbkj.common.sms.dto;

import com.zbkj.common.sms.AbstractSmsDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class VerifyCodeSmsDTO extends AbstractSmsDTO {
    /**
     * 必须含有该字段
     * 与模板枚举类（例如： AliyunSmsTemplateCode.java）中枚举的名称相同
     */
    private final static String BUSINESS = "verifyCode";

    /**
     * 必须含有该方法
     */
    public VerifyCodeSmsDTO() {
        setBusinessType(BUSINESS);
    }

    /** =============== 以下为业务字段 ================*/
    //短信验证码
    private String code;
}
