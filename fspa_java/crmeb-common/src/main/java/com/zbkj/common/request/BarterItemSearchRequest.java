package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 换物物品搜索请求对象（前台/后台共用）
 */
@Data
@ApiModel(value = "换物物品搜索请求对象", description = "换物物品搜索请求对象")
public class BarterItemSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "关键词（标题/描述）")
    private String keyword;

    @ApiModelProperty(value = "品类ID（末级）")
    private Integer categoryId;

    @ApiModelProperty(value = "状态：0下架 1在售 2审核中 3已驳回")
    private Integer status;

    @ApiModelProperty(value = "成色：1全新 2九新 3八新 4七新及以下 5其他")
    private Integer conditionLevel;

    @ApiModelProperty(value = "城市")
    private String city;

    @ApiModelProperty(value = "排序：1最新 2最热（按想要人数）")
    private Integer sort;

    @ApiModelProperty(value = "期望换取方式：0信用优先 1指定品类 2指定物品 3任意好物")
    private Integer expectType;

    @ApiModelProperty(value = "期望换取品类ID（expectType=1 时按此筛选）")
    private Integer expectCategoryId;

    @ApiModelProperty(value = "时间区间")
    private String dateLimit;
}
