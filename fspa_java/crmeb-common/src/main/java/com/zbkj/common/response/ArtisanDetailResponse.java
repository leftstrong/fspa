package com.zbkj.common.response;

import com.baomidou.mybatisplus.annotation.TableId;
import com.zbkj.common.vo.StoreOrderInfoOldVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
@ApiModel(value="StoreOrderDetailResponse对象", description="手艺人信息响应对象（pc列表用）")
public class ArtisanDetailResponse implements Serializable {

    private static final long serialVersionUID=1L;


    /**
     * ID
     */
    @TableId
    private Integer id;
    /**
     * 手机号码
     */
    private String phone;
    /**
     * 真实姓名
     */
    private String realName;
    /**
     * 用户昵称
     */
    private String nickname;
    /**
     * 用户头像
     */
    private String avatar;
    /**
     * 门店id
     */
    private Integer storeId;

    /**
     * 门店名称
     */
    private String storeName;
    /**
     * 专业
     */
    private String subject;
    /**
     * 1为正常，0为禁止
     */
    private Integer status;
    /**
     * 1为正常，0为禁止
     */
    private String statusName;

    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 工号
     */
    private String jobNumber;

    /**
     * 值班时间列表
     */
    private String workTime;
}
