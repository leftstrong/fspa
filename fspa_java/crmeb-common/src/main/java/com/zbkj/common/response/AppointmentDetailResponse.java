package com.zbkj.common.response;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

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
@ApiModel(value="AppointmentDetailResponse对象", description="预约信息响应对象（pc列表用）")
public class AppointmentDetailResponse implements Serializable {

    private static final long serialVersionUID=1L;


    /**
     * ID
     */
    @TableId
    private Integer id;
    /**
     * 用户id
     */
    private Integer uid;
    /**
     * 门店id
     */
    private Integer storeId;
    /**
     * 手艺人ID
     */
    private Integer artisanId;
    /**
     * 预约时间
     */
    private Date appointmentDate;

    /**
     * 预约时间
     */
    private String appointmentDateStr;
    /**
     * 商品ID字符串
     */
    private String productIds;
    /**
     * 核销码
     */
    private String verifyCode;
    /**
     * 1为正常，0为禁止
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 商品ID字符串
     */
    private String productNames;

    /**
     * 门店id
     */
    private String storeName;

    /**
     * 1为正常，0为禁止
     */
    private String statusName;

    /**
     * 用户id
     */
    private String uName;

    /**
     * 手艺人ID
     */
    private String artisanName;
}
