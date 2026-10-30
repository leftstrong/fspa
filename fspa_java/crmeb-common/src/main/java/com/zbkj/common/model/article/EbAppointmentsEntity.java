package com.zbkj.common.model.article;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 预约表 Entity 实体类
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2022 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * @author: 大粽子
 * +----------------------------------------------------------------------
 * @date Thu May 18 08:22:55 CST 2023
 * +----------------------------------------------------------------------
 * @email stivepeim@outlook.com
 * +----------------------------------------------------------------------
 */
@Data
@TableName("eb_appointments")
public class    EbAppointmentsEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
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
     * 商品ID字符串
     */
    private String productIds;
    /**
     * 核销码
     */
    private String verifyCode;
    /**
     * 0: 取消 1:正常
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

}
