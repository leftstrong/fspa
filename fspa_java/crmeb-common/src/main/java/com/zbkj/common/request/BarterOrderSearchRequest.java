package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 换物单搜索请求对象（后台）
 */
@Data
@ApiModel(value = "换物单搜索请求对象", description = "换物单搜索请求对象")
public class BarterOrderSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "换物单号")
    private String orderSn;

    @ApiModelProperty(value = "状态：0待确认 1已取消 2进行中 3待收货 4已完成 5已拒绝 6纠纷中")
    private Integer status;

    @ApiModelProperty(value = "物品主人用户ID")
    private Integer ownerUid;

    @ApiModelProperty(value = "申请人用户ID")
    private Integer applyUid;

    @ApiModelProperty(value = "参与用户ID：物主或申请人任一方匹配即可（后台按用户筛选换物单）")
    private Integer linkUid;

    @ApiModelProperty(value = "时间区间")
    private String dateLimit;
}
