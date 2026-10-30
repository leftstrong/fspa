package com.zbkj.common.response;

import com.zbkj.common.model.user.User;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.models.auth.In;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员详情顶部信息
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2022 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 */
@Data
public class TopDetail {

    private User user;
    // 余额
    private BigDecimal balance;
    // 积分
    private Integer integralCount;
    // 总计订单
    private Integer allOrderCount;
    // 本月订单
    private Integer mothOrderCount;
    // 总消费金额
    private BigDecimal allConsumeCount;
    // 本月消费金额
    private BigDecimal mothConsumeCount;
    // 当前消费金额
    private BigDecimal currentConsumeCount;
    // 当前订单数
    private Integer currentOrderCount;

    private Integer uid;
    private String nickname;
    private String phone;
    private BigDecimal nowMoney;
    private BigDecimal brokeragePrice;
    private Integer integral;
}
