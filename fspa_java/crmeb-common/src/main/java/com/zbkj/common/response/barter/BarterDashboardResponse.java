package com.zbkj.common.response.barter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 换物-数据驾驶舱 汇总指标
 * <p>
 * 所有 *Today 字段为「今日新增」，用于在指标卡上显示增量；
 * total 类为累计总量。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "BarterDashboardResponse", description = "换物数据驾驶舱汇总")
public class BarterDashboardResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /* ---------------- 用户 ---------------- */
    @ApiModelProperty(value = "换物用户总数（有换物行为或有积分/信用流水）")
    private Integer userTotal;
    @ApiModelProperty(value = "今日新增换物用户")
    private Integer userToday;
    @ApiModelProperty(value = "信用分平均值")
    private Double creditAvg;
    @ApiModelProperty(value = "信用分低于 60 的用户数（风险用户）")
    private Integer creditLowCount;

    /* ---------------- 物品 ---------------- */
    @ApiModelProperty(value = "物品总数（不含已删除）")
    private Integer itemTotal;
    @ApiModelProperty(value = "今日新增物品")
    private Integer itemToday;
    @ApiModelProperty(value = "在售")
    private Integer itemOnSale;
    @ApiModelProperty(value = "待审核")
    private Integer itemAuditing;
    @ApiModelProperty(value = "已换出")
    private Integer itemExchanged;
    @ApiModelProperty(value = "已下架")
    private Integer itemOff;

    /* ---------------- 换物单 ---------------- */
    @ApiModelProperty(value = "换物单总数")
    private Integer orderTotal;
    @ApiModelProperty(value = "今日新增换物单")
    private Integer orderToday;
    @ApiModelProperty(value = "进行中（已确认待交付/待收货）")
    private Integer orderTrading;
    @ApiModelProperty(value = "已完成")
    private Integer orderCompleted;
    @ApiModelProperty(value = "纠纷中")
    private Integer orderDispute;

    /* ---------------- 互动 ---------------- */
    @ApiModelProperty(value = "换物单留言总数（聊天记录）")
    private Integer messageTotal;
    @ApiModelProperty(value = "今日新增留言")
    private Integer messageToday;
    @ApiModelProperty(value = "站内通知总数")
    private Integer notifyTotal;
    @ApiModelProperty(value = "站内通知未读数")
    private Integer notifyUnread;

    /* ---------------- 治理 ---------------- */
    @ApiModelProperty(value = "待处理举报")
    private Integer reportPending;
    @ApiModelProperty(value = "举报总数")
    private Integer reportTotal;

    /* ---------------- 积分 ---------------- */
    @ApiModelProperty(value = "累计发放积分（正向流水合计）")
    private Integer pointsIssued;
    @ApiModelProperty(value = "累计消耗积分（负向流水合计）")
    private Integer pointsSpent;
    @ApiModelProperty(value = "当前流通积分（所有用户余额之和）")
    private Integer pointsBalance;

    /* ---------------- 派生 ---------------- */
    @ApiModelProperty(value = "成交率（已完成 / 换物单总数，百分比，保留 1 位）")
    private Double dealRate;
    @ApiModelProperty(value = "人均交换次数")
    private Double avgExchange;
}
