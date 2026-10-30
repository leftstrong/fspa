package com.zbkj.common.model.barter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 换物举报 Entity
 */
@Data
@TableName("eb_barter_report")
public class EbBarterReport implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer itemId;
    private Integer orderId;
    private Integer uid;
    private Integer targetUid;
    private Integer type;
    private String content;
    private Integer status;
    private String handleResult;
    private Integer handleUid;
    private Date createTime;
    private Date updateTime;
    private Date handleTime;
}
