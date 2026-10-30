package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 订单信息响应对象（pc列表用）
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
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="AppointmentSearchRequest对象", description="预约信息请求对象（pc列表用）")
public class AppointmentSearchRequest implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 门店id
     */
    private Integer storeId;
//    /**
//     * 专业
//     */
//    private String subject;
//    /**
//     * 1为正常，0为禁止
//     */
    private String verifyCode;

    @ApiModelProperty(value = "时间区间")
    private String dateLimit;
}
