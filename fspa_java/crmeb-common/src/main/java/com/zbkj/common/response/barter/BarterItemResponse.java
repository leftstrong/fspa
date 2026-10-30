package com.zbkj.common.response.barter;

import com.zbkj.common.model.barter.EbBarterItem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 换物物品响应对象
 * 在实体基础上附加「发布者信用/等级」信息，用于列表与详情的信任展示
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "换物物品响应对象", description = "含发布者信用与等级")
public class BarterItemResponse extends EbBarterItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "发布者昵称")
    private String ownerNickname;

    @ApiModelProperty(value = "发布者头像")
    private String ownerAvatar;

    @ApiModelProperty(value = "发布者等级名称")
    private String ownerLevelName;

    @ApiModelProperty(value = "发布者等级徽章")
    private String ownerLevelIcon;

    @ApiModelProperty(value = "发布者成功交换次数")
    private Integer ownerExchangeCount;

    @ApiModelProperty(value = "发布者信用分")
    private Integer ownerCreditScore;

    @ApiModelProperty(value = "是否置顶中")
    private Boolean toping;

    @ApiModelProperty(value = "当前登录用户是否标记过「我想要」")
    private Boolean isWant;

    @ApiModelProperty(value = "物品主人是否已设置默认收货地址（快递换物时用于前置校验）")
    private Boolean ownerHasDefaultAddr;

    @ApiModelProperty(value = "期望换取品类名称（由 expectCategoryId 解析，列表/详情直接展示，无需前端再查表）")
    private String expectCategoryName;
}
