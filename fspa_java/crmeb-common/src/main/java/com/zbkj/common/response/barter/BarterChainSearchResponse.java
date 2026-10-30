package com.zbkj.common.response.barter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 智能换物 - 链路检索结果
 *
 * 注意：检索阶段免费，只有「确认成环下单」才扣积分（见 BarterChainCreateResponse）。
 */
@Data
@ApiModel(value = "智能换物-链路检索结果", description = "候选交换环列表与可行性提示")
public class BarterChainSearchResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "是否找到可行环")
    private Boolean ok;

    @ApiModelProperty(value = "给用户的一句话结论（找不到时的原因说明也在这里）")
    private String tip;

    @ApiModelProperty(value = "我当前的换物积分余额")
    private Integer myPoints;

    @ApiModelProperty(value = "参与检索的在售物品数量（让用户知道检索范围）")
    private Integer poolSize;

    @ApiModelProperty(value = "目标物品主人是否愿意直接收下我的物品（= 1 跳能否成立；false 时说明必须有人中转，多跳仍可能成）")
    private Boolean targetAcceptsMe;

    @ApiModelProperty(value = "候选环列表，按跳数升序，最多 5 条")
    private List<Path> paths;

    /** 一条候选环 */
    @Data
    @ApiModel(value = "智能换物-候选环")
    public static class Path implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "跨越次数（= 环上人数 − 1）")
        private Integer hops;

        @ApiModelProperty(value = "该跳数成环所需积分")
        private Integer points;

        @ApiModelProperty(value = "一句话链路说明")
        private String summary;

        @ApiModelProperty(value = "是否涉及其它城市（跨城成环需要寄件）")
        private Boolean crossCity;

        @ApiModelProperty(value = "提示项：某方未设置默认收货地址、异地需寄件等")
        private List<String> warnings;

        @ApiModelProperty(value = "环上节点，按「物品流转顺序」排列：第 0 个是我的物品，最后一个是目标物品")
        private List<Node> nodes;
    }

    /** 环上一个参与者（持有一件物品） */
    @Data
    @ApiModel(value = "智能换物-环上节点")
    public static class Node implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "物品ID")
        private Integer itemId;

        @ApiModelProperty(value = "物品标题")
        private String title;

        @ApiModelProperty(value = "封面图")
        private String cover;

        @ApiModelProperty(value = "品类ID")
        private Integer categoryId;

        @ApiModelProperty(value = "品类名称")
        private String categoryName;

        @ApiModelProperty(value = "持有者用户ID")
        private Integer ownerUid;

        @ApiModelProperty(value = "持有者昵称")
        private String ownerNickname;

        @ApiModelProperty(value = "持有者头像")
        private String ownerAvatar;

        @ApiModelProperty(value = "物品所在城市")
        private String city;

        @ApiModelProperty(value = "成色文案")
        private String conditionText;

        @ApiModelProperty(value = "交付方式：1自提 2快递")
        private Integer deliveryType;

        @ApiModelProperty(value = "交付方式文案")
        private String deliveryText;

        @ApiModelProperty(value = "该物品主人想换什么（用于解释为什么会连上）")
        private String expectText;

        @ApiModelProperty(value = "角色：me=我的物品 target=目标物品 peer=中间人")
        private String role;

        @ApiModelProperty(value = "交接说明：给出什么、从谁手里得到什么")
        private String handoff;
    }
}
