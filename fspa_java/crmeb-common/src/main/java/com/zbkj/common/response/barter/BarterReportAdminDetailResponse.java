package com.zbkj.common.response.barter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 举报-后台明细（点击举报行弹出查看）
 * <p>
 * 处理一条举报要判断三件事：物品到底是什么（图 + 描述 + 当前状态）、
 * 举报人是什么人（信用分 / 历史被举报次数）、被举报人是什么人。
 * 这些信息分散在 4 张表，后台一次给全，避免运营来回跳页。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "BarterReportAdminDetailResponse", description = "举报后台明细")
public class BarterReportAdminDetailResponse implements Serializable {

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

    @ApiModelProperty(value = "举报成立时扣减的信用分（取自规则配置，0 表示不扣）")
    private Integer creditPenalty;
    @ApiModelProperty(value = "举报成立时是否会自动下架被举报物品")
    private Boolean autoOffshelf;

    @ApiModelProperty(value = "被举报物品")
    private Item item;
    @ApiModelProperty(value = "关联换物单，可为空")
    private Order order;
    @ApiModelProperty(value = "举报人")
    private Party reporter;
    @ApiModelProperty(value = "被举报人")
    private Party target;

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    @ApiModel(value = "BarterReportAdminDetailResponse.Item")
    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer id;
        private String title;
        private String description;
        private String images;
        private String cover;
        private String city;
        private Integer conditionLevel;
        private String conditionText;
        private Integer status;
        private String statusText;
        private Integer ownerUid;
        private String ownerNickname;
        private Integer viewCount;
        private Integer wantCount;
        private Date createTime;
        /** 该物品历史上被举报过多少次（含本次） */
        private Integer reportCount;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    @ApiModel(value = "BarterReportAdminDetailResponse.Order")
    public static class Order implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer id;
        private String orderSn;
        private Integer status;
        private String statusText;
        private Integer deliveryType;
        private String deliveryTypeText;
        private Date createTime;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    @ApiModel(value = "BarterReportAdminDetailResponse.Party")
    public static class Party implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer uid;
        private String nickname;
        private String avatar;
        private String phone;
        private Integer status;
        private Integer creditScore;
        private Integer points;
        private Integer exchangeCount;
        private Integer levelId;
        private String levelName;
        private Date createTime;
        /** 该用户累计举报过几次（对 reporter 有意义） */
        private Integer reportTotal;
        /** 该用户累计被举报几次（对 target 有意义） */
        private Integer reportedTotal;
        @ApiModelProperty(value = "该用户名下在售物品数")
        private Integer onSaleCount;
    }

    @ApiModelProperty(value = "同一被举报人的其他待处理举报，便于合并判断")
    private List<BarterReportAdminResponse> relatedReports;
}
