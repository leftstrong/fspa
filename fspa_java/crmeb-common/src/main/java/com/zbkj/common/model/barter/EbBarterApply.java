package com.zbkj.common.model.barter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 换物申请/提案 Entity
 */
@Data
@TableName("eb_barter_apply")
public class EbBarterApply implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer itemId;
    private Integer applyUid;
    private Integer ownerUid;
    private String offerItemIds;
    private String message;
    private Integer status;
    private Date createTime;
    private Date updateTime;

    /* ---- 以下为列表展示用的附加字段，不参与建表与写入 ---- */
    /** 目标物品标题（列表/详情带出，避免前端只能显示 itemId） */
    @TableField(exist = false)
    private String itemTitle;
    /** 目标物品封面图首张 */
    @TableField(exist = false)
    private String itemCover;
    /** 申请人昵称 */
    @TableField(exist = false)
    private String applyNickname;
    /** 申请人头像 */
    @TableField(exist = false)
    private String applyAvatar;
}
