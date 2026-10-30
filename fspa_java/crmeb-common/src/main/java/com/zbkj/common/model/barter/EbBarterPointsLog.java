package com.zbkj.common.model.barter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 换物积分日志 Entity
 */
@Data
@TableName("eb_barter_points_log")
public class EbBarterPointsLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer uid;
    private Integer type;
    private Integer value;
    private Integer balance;
    private String mark;
    private Integer linkType;
    private Integer linkId;
    private Date createTime;
}
