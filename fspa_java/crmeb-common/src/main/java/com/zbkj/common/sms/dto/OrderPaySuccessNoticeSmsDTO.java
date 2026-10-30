package com.zbkj.common.sms.dto;

import com.zbkj.common.sms.AbstractSmsDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class OrderPaySuccessNoticeSmsDTO extends AbstractSmsDTO {

    // 业务类型
    private final static String BUSINESS = "adminPaySuccess";

    public OrderPaySuccessNoticeSmsDTO() {
        setBusinessType(BUSINESS);
    }

    //订单号
    private String order_id;
    //管理员名称
    private String admin_name;


}
