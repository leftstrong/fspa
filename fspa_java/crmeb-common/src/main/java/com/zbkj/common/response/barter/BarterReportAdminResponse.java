package com.zbkj.common.response.barter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 举报-后台列表行（运营视角）
 * <p>
 * 后台看举报，最需要三样东西：被举报的是哪件物品（图和名）、谁举报的、被谁举报的。
 * 只给 uid 和 itemId 等于让运营去别的页面反查，所以这里一次性富化到位。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "BarterReportAdminResponse", description = "举报后台列表行")
public class BarterReportAdminResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "举报 ID")
    private Integer id;

    @ApiModelProperty(value = "类型：1物品违规 2用户违规 3欺诈 4其他")
    private Integer type;
    @ApiModelProperty(value = "类型文案")
    private String typeText;

    @ApiModelProperty(value = "举报内容")
    private String content;

    @ApiModelProperty(value = "状态：0待处理 1已处理 2已驳回")
    private Integer status;
    @ApiModelProperty(value = "状态文案")
    private String statusText;

    /* ---------------- 被举报物品 ---------------- */
    @ApiModelProperty(value = "物品 ID")
    private Integer itemId;
    @ApiModelProperty(value = "物品标题")
    private String itemTitle;
    @ApiModelProperty(value = "物品封面")
    private String itemCover;
    @ApiModelProperty(value = "物品图集（逗号分隔）")
    private String itemImages;
    @ApiModelProperty(value = "物品当前状态值")
    private Integer itemStatus;
    @ApiModelProperty(value = "物品当前状态文案")
    private String itemStatusText;

    /* ---------------- 关联换物单 ---------------- */
    @ApiModelProperty(value = "换物单 ID")
    private Integer orderId;
    @ApiModelProperty(value = "换物单号")
    private String orderSn;
    @ApiModelProperty(value = "换物单状态文案")
    private String orderStatusText;

    /* ---------------- 举报人 ---------------- */
    @ApiModelProperty(value = "举报人 UID")
    private Integer uid;
    @ApiModelProperty(value = "举报人昵称")
    private String nickname;
    @ApiModelProperty(value = "举报人头像")
    private String avatar;

    /* ---------------- 被举报人 ---------------- */
    @ApiModelProperty(value = "被举报人 UID")
    private Integer targetUid;
    @ApiModelProperty(value = "被举报人昵称")
    private String targetNickname;
    @ApiModelProperty(value = "被举报人头像")
    private String targetAvatar;

    /* ---------------- 时间与处理 ---------------- */
    @ApiModelProperty(value = "举报时间")
    private Date createTime;
    @ApiModelProperty(value = "处理时间")
    private Date handleTime;
    @ApiModelProperty(value = "处理说明")
    private String handleResult;
    @ApiModelProperty(value = "处理人 UID")
    private Integer handleUid;
    @ApiModelProperty(value = "处理人账号")
    private String handleUidName;
}
