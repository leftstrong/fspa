package com.zbkj.common.model.barter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 换物信用分日志 Entity
 * 信用分此前只改 eb_user.credit_score，不留痕、无法回溯明细；新增本表记录每一次增减。
 */
@Data
@TableName("eb_barter_credit_log")
public class EbBarterCreditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer uid;
    /** 1 增加 2 扣减 */
    private Integer type;
    private Integer value;
    private Integer balance;
    private String mark;
    /** 1 换物完成 2 互评 3 平台调整 */
    private Integer linkType;
    private Integer linkId;
    private Date createTime;
}
