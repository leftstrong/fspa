package com.zbkj.common.sms.dto;

import com.zbkj.common.sms.AbstractSmsDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class PaySuccessSmsDTO extends AbstractSmsDTO {

    // 业务类型
    private final static String BUSINESS = "paySuccess";

    public PaySuccessSmsDTO() {
        setBusinessType(BUSINESS);
    }

    //订单号
    private String order_id;
    //支付金额
    private String pay_price;


}
