package com.zbkj.common.response.barter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 换物未读消息汇总（供 tabBar / 换物页各条目标红显示数字）
 */
@Data
@ApiModel(value = "BarterUnreadResponse", description = "换物未读消息汇总")
public class BarterUnreadResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "全部未读")
    private Integer total = 0;

    @ApiModelProperty(value = "收到的申请 未读")
    private Integer incoming = 0;

    @ApiModelProperty(value = "我发出的申请 未读")
    private Integer outgoing = 0;

    @ApiModelProperty(value = "换物单 未读")
    private Integer order = 0;

    @ApiModelProperty(value = "想要/被想要 未读")
    private Integer want = 0;

    @ApiModelProperty(value = "系统通知 未读")
    private Integer system = 0;
}
