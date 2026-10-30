package com.zbkj.common.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * @author Chennong
 * @date 2023-06-18
 * @description 手艺人信息相应对象
 */
@Data
public class EbArtisanInfoResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
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

    /**
     * 值班时间列表
     */
    private Map<String, List<String>> workTimeMap;

    /**
     * 值班时间列表
     */
    private List<Map<String, Object>> workTimeMapList;

    /**
     * 值班时间列表
     */
    private List<Map<String, Object>> emptyWorkTimeMapList;

    private List<Integer> expandedKeys;

    private List<Integer> checkedKeys;
}
