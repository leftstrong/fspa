package com.zbkj.common.response.barter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 智能换物 - 成环下单结果
 */
@Data
@ApiModel(value = "智能换物-成环下单结果", description = "环号、实扣积分与生成的订单列表")
public class BarterChainCreateResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "环号（同一次成环的 N 张订单共用）")
    private String chainNo;

    @ApiModelProperty(value = "环上人数（2 人环两人共用一张单，不是订单张数）")
    private Integer chainSize;

    @ApiModelProperty(value = "实际生成的 1v1 订单张数（2 人环 = 1，3 人以上 = 人数）")
    private Integer orderCount;

    @ApiModelProperty(value = "跨越次数（= 环上人数 − 1）")
    private Integer hops;

    @ApiModelProperty(value = "路径规划费（成交时收取；本次发起不扣）")
    private Integer points;

    @ApiModelProperty(value = "本次实际扣除的积分（只走第一棒的模型下，发起时恒为 0）")
    private Integer charged;

    @ApiModelProperty(value = "积分余额（发起时不扣分，原样返回）")
    private Integer balance;

    @ApiModelProperty(value = "生成的订单ID列表（只走第一棒，恒为 1 张）")
    private List<Integer> orderIds;

    @ApiModelProperty(value = "提示项（有人未设置默认收货地址等，不阻断成环但要提醒）")
    private List<String> warnings;

    /* ------------ 环详情（供下单后直接跳转到环进度页） ------------ */

    @Data
    @ApiModel(value = "智能换物-环详情")
    public static class Detail implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "环号")
        private String chainNo;

        @ApiModelProperty(value = "环上人数")
        private Integer chainSize;

        @ApiModelProperty(value = "跨越次数")
        private Integer hops;

        @ApiModelProperty(value = "环整体状态文案：待确认/进行中/已完成/已解散")
        private String statusText;

        @ApiModelProperty(value = "是否已解散（任一单被拒/取消）")
        private Boolean dissolved;

        @ApiModelProperty(value = "环上订单（按链表顺序）")
        private List<Edge> edges;
    }

    /** 环上的一张 1v1 订单 */
    @Data
    @ApiModel(value = "智能换物-环上订单")
    public static class Edge implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "订单ID")
        private Integer orderId;

        @ApiModelProperty(value = "换物单号")
        private String orderSn;

        @ApiModelProperty(value = "序号，从 1 开始")
        private Integer seq;

        @ApiModelProperty(value = "我给出什么（物品标题）")
        private String fromItemTitle;

        @ApiModelProperty(value = "我得到什么（物品标题）")
        private String toItemTitle;

        @ApiModelProperty(value = "给出方昵称")
        private String fromNickname;

        @ApiModelProperty(value = "得到方/物主昵称")
        private String toNickname;

        @ApiModelProperty(value = "订单状态：0待确认 1已取消 2进行中 3待收货 4已完成 5已拒绝 6纠纷中")
        private Integer status;

        @ApiModelProperty(value = "订单状态文案")
        private String statusText;

        @ApiModelProperty(value = "当前登录用户在这张单里的角色：owner/apply/none")
        private String myRole;

        @ApiModelProperty(value = "创建时间")
        private Date createTime;
    }
}
