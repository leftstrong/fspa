package com.zbkj.common.sms.dto;

import com.zbkj.common.sms.AbstractSmsDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class ArtisanNoticeSmsDTO extends AbstractSmsDTO {

    // 业务类型
    private final static String BUSINESS = "artisanNotice";

    public ArtisanNoticeSmsDTO() {
        setBusinessType(BUSINESS);
    }

    //预约时间
    private String appointment_date;
    //手艺人名称
    private String real_name;


}
