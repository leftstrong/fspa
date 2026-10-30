package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 智能换物 - 成环下单请求
 *
 * ringItemIds 必须按「物品流转顺序」传完整环：[我的物品A, 中间人1, …, 目标物品B]，
 * 后端会重新校验每一件是否仍在售、每一段是否仍然成立，不信任客户端的链路结果。
 */
@Data
@ApiModel(value = "智能换物-成环下单请求对象", description = "按选定的交换环生成 N 张 1v1 换物单")
public class BarterChainCreateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "我拿出的物品ID（环的第 0 个节点）")
    private Integer myItemId;

    @ApiModelProperty(value = "我想要的物品ID（环的最后一个节点）")
    private Integer targetItemId;

    @ApiModelProperty(value = "完整环的物品ID列表，顺序：我的物品 → 中间人 → 目标物品")
    private List<Integer> ringItemIds;

    @ApiModelProperty(value = "用户在检索时选择的最大跨越次数（1/2/3），用于校验实际跳数未超出所选档位")
    private Integer maxHops;

    @ApiModelProperty(value = "我的收货地址ID（走快递时使用）")
    private Integer addressId;

    @ApiModelProperty(value = "附言（会写进我发起的那张单）")
    private String applyMessage;
}
