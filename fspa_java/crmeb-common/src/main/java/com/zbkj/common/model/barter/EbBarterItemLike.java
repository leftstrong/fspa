package com.zbkj.common.model.barter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 「我想要」记录 Entity（轻点赞：表达换取意愿，可参与热度排序）
 */
@Data
@TableName("eb_barter_item_like")
public class EbBarterItemLike implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer itemId;
    private Integer uid;
    private Date createTime;
}
