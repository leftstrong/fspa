package com.zbkj.common.model.barter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 闲置物品（好物）Entity
 */
@Data
@TableName("eb_barter_item")
public class EbBarterItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer uid;
    private Integer categoryId;
    private Integer categoryPid;
    private String title;
    private String images;
    private String description;
    private Integer conditionLevel;
    private String city;
    private Integer expectType;
    private String expectDesc;
    private Integer expectCategoryId;
    /** 交付方式 1自提 2快递（发布时选择） */
    private Integer deliveryType;
    /**
     * 通过哪张换物单收到的这件物品（非空 = 换物接力收到的，物品所有权已转到当前 uid 名下）。
     * 「我的物品」里据此给一个「继续换下一件」的入口，用户点了才重新上架。
     */
    private Integer receiveFromOrderId;
    private String tags;
    private Integer status;
    private Date topEndTime;
    private Integer topPoints;
    private Integer viewCount;
    private Integer likeCount;
    private Integer wantCount;
    /** 驳回原因（审核驳回时由后台填写，驳回记录可追溯） */
    private String rejectReason;
    private Date createTime;
    private Date updateTime;
}
