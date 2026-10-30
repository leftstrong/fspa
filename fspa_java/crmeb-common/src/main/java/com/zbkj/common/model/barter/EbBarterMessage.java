package com.zbkj.common.model.barter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 以物换物 站内消息
 * uid = 接收者；bizId 关联业务主键（applyId / orderId / itemId）
 */
@Data
@TableName("eb_barter_message")
public class EbBarterMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    /** 接收者 */
    private Integer uid;
    /** 消息类型，见 BarterConstants.MSG_* */
    private Integer type;
    /** 业务分组：incoming/outgoing/order/want/system */
    private String groupType;
    /** 关联业务ID */
    private Integer bizId;
    private String title;
    private String content;
    /** 是否已读 0未读 1已读 */
    private Integer isRead;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date readTime;
}
