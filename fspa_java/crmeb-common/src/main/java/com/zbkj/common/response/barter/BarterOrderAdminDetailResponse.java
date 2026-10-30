package com.zbkj.common.response.barter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 换物单-后台详情（运营视角）
 * <p>
 * 与前台 BarterOrderDetailResponse 的区别：不做「我 / 对方」视角折叠，
 * 双方信息、双方物流、聊天记录、互评全部平铺，供客服排障与纠纷处理。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "BarterOrderAdminDetailResponse", description = "换物单后台详情")
public class BarterOrderAdminDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "换物单 ID")
    private Integer id;
    @ApiModelProperty(value = "单号")
    private String orderSn;
    @ApiModelProperty(value = "状态：0待确认 1已取消 2进行中 3待收货 4已完成 5已拒绝 6纠纷中")
    private Integer status;
    @ApiModelProperty(value = "状态文案")
    private String statusText;
    @ApiModelProperty(value = "交付方式：1自提 2快递")
    private Integer deliveryType;
    @ApiModelProperty(value = "交付方式文案")
    private String deliveryTypeText;
    @ApiModelProperty(value = "自提地址")
    private String selfTakeAddress;
    @ApiModelProperty(value = "申请人附言")
    private String applyMessage;

    @ApiModelProperty(value = "目标物品（物主要换出的那件）")
    private Item item;
    @ApiModelProperty(value = "申请人提供的物品")
    private List<Item> offerItems;

    @ApiModelProperty(value = "物主（发起方）")
    private Party owner;
    @ApiModelProperty(value = "申请人")
    private Party applyUser;

    @ApiModelProperty(value = "环编号（智能换物）")
    private String chainNo;
    @ApiModelProperty(value = "环内序号")
    private Integer chainSeq;
    @ApiModelProperty(value = "环总人数")
    private Integer chainSize;
    @ApiModelProperty(value = "路径规划费应扣积分")
    private Integer pathFeePoints;
    @ApiModelProperty(value = "已实扣积分")
    private Integer pathFeeCharged;
    @ApiModelProperty(value = "是否已结算")
    private Integer pathFeePaid;
    @ApiModelProperty(value = "建单时冻结积分")
    private Integer pathFeeFrozen;

    @ApiModelProperty(value = "聊天记录（按时间正序）")
    private List<Chat> messages;
    @ApiModelProperty(value = "互评记录")
    private List<Evaluate> evaluates;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;
    @ApiModelProperty(value = "完成时间")
    private Date completeTime;

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    @ApiModel(value = "BarterOrderAdminDetailResponse.Item")
    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer id;
        private String title;
        private String images;
        private String cover;
        private String city;
        private Integer conditionLevel;
        private String conditionText;
        private Integer status;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    @ApiModel(value = "BarterOrderAdminDetailResponse.Party")
    public static class Party implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer uid;
        private String nickname;
        private String avatar;
        private String phone;
        private Integer creditScore;
        private Integer exchangeCount;
        private Integer levelId;
        private String levelName;
        /** 该侧收货地址 */
        private String address;
        /** 该侧发货状态：0未发货 1已发货 */
        private Integer deliverStatus;
        private String logisticsCompany;
        private String logisticsCompanyName;
        private String logisticsNo;
        private Date deliverTime;
        /** 该侧是否已确认收货 */
        private Integer confirmRecv;
        /** 改单号次数 */
        private Integer logisticsEdit;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    @ApiModel(value = "BarterOrderAdminDetailResponse.Chat")
    public static class Chat implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer id;
        private Integer fromUid;
        private String fromNickname;
        private Integer toUid;
        private Integer type;
        private String typeText;
        private String content;
        private String address;
        private Date createTime;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    @ApiModel(value = "BarterOrderAdminDetailResponse.Evaluate")
    public static class Evaluate implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer id;
        private Integer uid;
        private String nickname;
        private Integer toUid;
        private String toNickname;
        private Integer star;
        private String content;
        private Date createTime;
    }
}
