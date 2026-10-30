package com.zbkj.common.sms.dto;

import com.zbkj.common.sms.AbstractSmsDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Setter
@Getter
@Accessors(chain = true)
public class OrderEditPriceNoticeSmsDTO extends AbstractSmsDTO {

    // 业务类型
    private final static String BUSINESS = "modifyOrderPrice";

    public OrderEditPriceNoticeSmsDTO() {
        setBusinessType(BUSINESS);
    }

    //订单号
    private String order_id;
    //改价金额
    private String pay_price;
}
