package com.zbkj.common.response.barter;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 换物申请详情（消息点击后直达的二级页数据）
 *
 * 相比列表接口，额外带出：目标物品、对方用来交换的物品、双方用户摘要、
 * 当前用户在该申请中的角色与可执行操作权限。
 */
@Data
@ApiModel(value = "BarterApplyDetailResponse", description = "换物申请详情")
public class BarterApplyDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("申请ID")
    private Integer id;

    @ApiModelProperty("目标物品ID")
    private Integer itemId;

    @ApiModelProperty("申请人UID")
    private Integer applyUid;

    @ApiModelProperty("物主UID")
    private Integer ownerUid;

    @ApiModelProperty("申请人留言")
    private String message;

    @ApiModelProperty("状态 0已取消 1待确认 2已接受 3已拒绝 4已生成换物单")
    private Integer status;

    @ApiModelProperty("状态文案")
    private String statusText;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @ApiModelProperty("目标物品")
    private SimpleItem item;

    @ApiModelProperty("申请人用来交换的物品")
    private List<SimpleItem> offerItems;

    @ApiModelProperty("申请人信息")
    private SimpleUser applyUser;

    @ApiModelProperty("物主信息")
    private SimpleUser ownerUser;

    @ApiModelProperty("当前用户角色：owner=物主 apply=申请人")
    private String myRole;

    @ApiModelProperty("我是否是物主（决定是否显示接受/拒绝）")
    private Boolean canAccept;

    @ApiModelProperty("我是否是申请人（决定是否显示取消）")
    private Boolean canCancel;

    @ApiModelProperty("已生成的换物单ID，未生成则为空")
    private Integer orderId;

    @Data
    @ApiModel("申请详情-物品简版")
    public static class SimpleItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer id;
        private String title;
        private String images;
        private String cover;
        private String city;
        private Integer conditionLevel;
        private String conditionText;
    }

    @Data
    @ApiModel("申请详情-用户简版")
    public static class SimpleUser implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer uid;
        private String nickname;
        private String avatar;
        private Integer exchangeCount;
        private Integer creditScore;
        private Integer levelId;
    }
}
