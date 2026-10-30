package com.zbkj.common.response.barter;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 换物单详情（消息点击后直达的二级页数据）
 *
 * 相比列表接口，额外带出：双方物品、双方用户、交付方式与物流进度、
 * 「我」的视角（我的发货状态 / 我的收货确认 / 我的地址）与可执行操作。
 */
@Data
@ApiModel(value = "BarterOrderDetailResponse", description = "换物单详情")
public class BarterOrderDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("换物单ID")
    private Integer id;

    @ApiModelProperty("换物单号")
    private String orderSn;

    @ApiModelProperty("来源申请ID")
    private Integer applyId;

    @ApiModelProperty("状态 0待确认 1已取消 2进行中(快递=待发货/自提=待完成) 3待收货 4已完成 5已拒绝 6纠纷中")
    private Integer status;

    @ApiModelProperty("状态文案")
    private String statusText;

    @ApiModelProperty("交付方式 1自提 2快递")
    private Integer deliveryType;

    @ApiModelProperty("交付方式文案")
    private String deliveryTypeText;

    @ApiModelProperty("自提点")
    private String selfTakeAddress;

    @ApiModelProperty("当前用户角色：owner=物主 apply=申请人")
    private String myRole;

    @ApiModelProperty("对方角色文案，如「物主」「申请人」")
    private String peerRoleText;

    @ApiModelProperty("物主的物品")
    private SimpleItem item;

    @ApiModelProperty("申请人用来交换的物品")
    private List<SimpleItem> offerItems;

    @ApiModelProperty("物主信息")
    private SimpleUser ownerUser;

    @ApiModelProperty("申请人信息")
    private SimpleUser applyUser;

    @ApiModelProperty("来源换物申请的留言（申请人发起时填写）")
    private String applyMessage;

    @ApiModelProperty("我方发货状态 0未发 1已发")
    private Integer myDeliverStatus;

    @ApiModelProperty("对方发货状态 0未发 1已发")
    private Integer peerDeliverStatus;

    @ApiModelProperty("我方物流公司编码")
    private String myLogisticsCompany;

    @ApiModelProperty("我方物流公司名")
    private String myLogisticsCompanyName;

    @ApiModelProperty("我方快递单号")
    private String myLogisticsNo;

    @ApiModelProperty("对方物流公司名")
    private String peerLogisticsCompanyName;

    @ApiModelProperty("对方快递单号")
    private String peerLogisticsNo;

    @ApiModelProperty("我方收货地址快照")
    private String myAddress;

    @ApiModelProperty("对方收货地址快照")
    private String peerAddress;

    @ApiModelProperty("我方是否已确认收货")
    private Integer myConfirmRecv;

    @ApiModelProperty("对方是否已确认收货")
    private Integer peerConfirmRecv;

    @ApiModelProperty("我是否可以发货")
    private Boolean canDeliver;

    @ApiModelProperty("我是否可以确认收货/完成")
    private Boolean canConfirm;

    @ApiModelProperty("我是否可以接受（仅物主且待确认）")
    private Boolean canAccept;

    @ApiModelProperty("我是否可以拒绝（仅物主且待确认）")
    private Boolean canReject;

    @ApiModelProperty("我是否可以取消（待确认，或进行中且双方都还没发货）")
    private Boolean canCancel;

    @ApiModelProperty("不能取消时的原因说明（给用户看，别只把按钮变灰）")
    private String cancelBlockedTip;

    @ApiModelProperty("我是否可以修改自己这一侧的快递单号（发货后 24 小时内、最多 3 次）")
    private Boolean canUpdateExpress;

    @ApiModelProperty("修改单号的剩余条件说明，如「发货后 24 小时内可修改，还剩 23 小时」")
    private String expressEditTip;

    @ApiModelProperty("我是否可以发起纠纷（进行中或待收货，任意方）")
    private Boolean canDispute;

    @ApiModelProperty("智能换物环号（空串=普通 1v1 换物）")
    private String chainNo;

    @ApiModelProperty("本单在环上的序号（1 起）")
    private Integer chainSeq;

    @ApiModelProperty("环上人数（= 环上订单张数；1 表示不是环）")
    private Integer chainSize;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @ApiModelProperty("完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date completeTime;

    @Data
    @ApiModel("换物单详情-物品简版")
    public static class SimpleItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer id;
        private String title;
        private String images;
        private String cover;
        private String city;
        private Integer conditionLevel;
        private String conditionText;
    }

    @Data
    @ApiModel("换物单详情-用户简版")
    public static class SimpleUser implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer uid;
        private String nickname;
        private String avatar;
        private Integer exchangeCount;
        private Integer creditScore;
        private Integer levelId;
    }
}
