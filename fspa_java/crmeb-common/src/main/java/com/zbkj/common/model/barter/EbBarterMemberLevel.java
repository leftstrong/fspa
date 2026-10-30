package com.zbkj.common.model.barter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 换物会员等级配置 Entity
 * 等级由「成功交换次数」自动匹配：min_count <= exchangeCount < max_count（max_count=0 表示无上限）
 */
@Data
@TableName("eb_barter_member_level")
public class EbBarterMemberLevel implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String icon;
    private Integer minCount;
    private Integer maxCount;
    private Integer exposureWeight;
    private String badgeColor;
    private Integer sort;
    private Integer autoMatch;
    private Date createTime;
    private Date updateTime;
}
