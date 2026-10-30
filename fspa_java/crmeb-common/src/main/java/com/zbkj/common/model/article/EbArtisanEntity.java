package com.zbkj.common.model.article;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 匠人表 Entity 实体类
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2022 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * @author: 大粽子
 * +----------------------------------------------------------------------
 * @date Thu May 18 08:44:58 CST 2023
 * +----------------------------------------------------------------------
 * @email stivepeim@outlook.com
 * +----------------------------------------------------------------------
 */
@Data
@TableName("eb_artisan")
public class EbArtisanEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;
    /**
     * 手机号码
     */
    private String phone;
    /**
     * 真实姓名
     */
    private String realName;
    /**
     * 用户昵称
     */
    private String nickname;
    /**
     * 用户头像
     */
    private String avatar;
    /**
     * 门店id
     */
    private Integer storeId;
    /**
     * 专业
     */
    private String subject;
    /**
     * 1为正常，0为禁止
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 工号
     */
    private String jobNumber;

    /**
     * 值班时间列表
     */
    private String workTime;

}
