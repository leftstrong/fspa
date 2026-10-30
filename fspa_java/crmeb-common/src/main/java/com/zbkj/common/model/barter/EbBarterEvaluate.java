package com.zbkj.common.model.barter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 换物互评 Entity
 */
@Data
@TableName("eb_barter_evaluate")
public class EbBarterEvaluate implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer orderId;
    private Integer uid;
    private Integer toUid;
    private Integer itemId;
    private Integer star;
    private String content;
    private String images;
    private Date createTime;
}
