package com.zbkj.common.model.barter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 换物单 Entity
 */
@Data
@TableName("eb_barter_order")
public class EbBarterOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String orderSn;
    private Integer applyId;
    private Integer itemId;
    private Integer ownerUid;
    private Integer applyUid;
    private String offerItemIds;
    private Integer status;
    private Integer deliveryType;
    private Integer ownerDeliverStatus;
    private Integer applyDeliverStatus;
    private String ownerLogisticsNo;
    private String applyLogisticsNo;
    /** 快递公司编码（见 BarterConstants.EXPRESS_LIST） */
    private String ownerLogisticsCompany;
    private String applyLogisticsCompany;
    /** 物主/申请人发货时间：用于「发货后 24 小时内可自查修改单号」 */
    private Date ownerDeliverTime;
    private Date applyDeliverTime;
    /** 单号被本人修改过几次（上限见 BarterConstants.EXPRESS_EDIT_MAX） */
    private Integer ownerLogisticsEdit;
    private Integer applyLogisticsEdit;
    private String selfTakeAddress;
    private String ownerAddress;
    private String applyAddress;
    /** 申请人附言（发起换物时填写） */
    private String applyMessage;
    /** 申请人收货地址ID（快递模式下快照用） */
    private Integer applicantAddressId;
    private Integer ownerConfirmRecv;
    private Integer applyConfirmRecv;
    private Integer ownerEvaluateId;
    private Integer applyEvaluateId;
    /**
     * 智能换物-方案号（空=普通 1v1 换物）。
     * 只给方案 + 只走第一棒的模型下，同一套方案只会生成 1 张单（chainSeq=1），
     * 后续几棒由用户自己发起普通换物，因此这里仅作「这套方案」的标识。
     */
    private String chainNo;
    /** 智能换物-本单是方案里的第几棒（只走第一棒时恒为 1） */
    private Integer chainSeq;
    /** 智能换物-方案总人数（用于详情页展示「第 1/N 棒」） */
    private Integer chainSize;
    /** 路径规划费：本单成交时应收的积分（0=普通换物单，不收） */
    private Integer pathFeePoints;
    /** 路径规划费：成交时实际扣到的积分（余额不足时可能小于应收） */
    private Integer pathFeeCharged;
    /** 路径规划费是否已结算：0 未结算 / 1 已结算（成交时才结算，失败不收费） */
    private Integer pathFeePaid;
    /** 路径规划费：发起时已冻结的积分（成交转实扣 / 未成交按此解冻，余额不足时小于应收） */
    private Integer pathFeeFrozen;
    private Date createTime;
    private Date updateTime;
    private Date completeTime;
}
