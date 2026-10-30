package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 智能换物 - 链路检索请求
 *
 * 场景：用户拿出自己的一件在售物 A，指定平台上一件在售物 B，
 * 系统在「在售物品图」里找一条 A → X1 → … → B → A 的交换环（B 的主人拿 A）。
 *
 * 跳数语义：1 跳 = 两人直接互换；2 跳 = 3 人环；3 跳 = 4 人环。
 * 跳数 = 环上人数 − 1，与积分档位一一对应（1/2/3 跳 → 10/20/50 积分）。
 */
@Data
@ApiModel(value = "智能换物-链路检索请求对象", description = "以我的在售物 A 为起点、想要的在售物 B 为终点检索交换环")
public class BarterChainSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "我拿出的物品ID（必须是我自己的在售物）")
    private Integer myItemId;

    @ApiModelProperty(value = "我想要的物品ID（平台在售物，不能是自己的）")
    private Integer targetItemId;

    @ApiModelProperty(value = "允许的最大跨越次数：1/2/3，缺省 3；仅影响搜索深度，检索本身不扣积分")
    private Integer maxHops;
}
