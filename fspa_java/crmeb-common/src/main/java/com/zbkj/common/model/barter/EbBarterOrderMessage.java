package com.zbkj.common.model.barter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 换物单内聊天消息（仅换物双方可见，绑定 orderId）
 * type: 1文本 2图片 3定位
 */
@Data
@TableName("eb_barter_order_message")
public class EbBarterOrderMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer orderId;
    private Integer fromUid;
    private Integer toUid;
    /** 1文本 2图片 3定位 */
    private Integer type;
    private String content;
    private String lat;
    private String lng;
    private String address;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
