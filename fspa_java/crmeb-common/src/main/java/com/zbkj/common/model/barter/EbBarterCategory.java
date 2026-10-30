package com.zbkj.common.model.barter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 换物-品类 Entity
 */
@Data
@TableName("eb_barter_category")
public class EbBarterCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer pid;
    private String name;
    private String icon;
    private Integer sort;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
