package com.zbkj.common.sms.dto;

import com.zbkj.common.sms.AbstractSmsDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class OrderDeliverNoticeSmsDTO extends AbstractSmsDTO {

    // 业务类型
    private final static String BUSINESS = "deliverGoods";

    public OrderDeliverNoticeSmsDTO() {
        setBusinessType(BUSINESS);
    }

    //订单号
    private String order_id;
    //商品名称
    private String store_name;
    //用户名称
    private String nickname;
}
