package com.zbkj.common.response.barter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 换物-用户信用/积分看板 汇总响应（后台）
 */
@Data
@ApiModel(value = "换物-用户信用/积分看板汇总响应")
public class BarterUserStatResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户ID")
    private Integer uid;
    @ApiModelProperty(value = "昵称")
    private String nickname;
    @ApiModelProperty(value = "头像")
    private String avatar;
    @ApiModelProperty(value = "成功交换次数（会员等级锚点）")
    private Integer exchangeCount;
    @ApiModelProperty(value = "当前会员等级ID")
    private Integer levelId;
    @ApiModelProperty(value = "当前会员等级名称")
    private String levelName;
    @ApiModelProperty(value = "所在城市")
    private String city;
    @ApiModelProperty(value = "积分余额")
    private Integer points;
    @ApiModelProperty(value = "冻结积分（发起智能换物时冻结）")
    private Integer pointsFrozen;
    @ApiModelProperty(value = "信用分")
    private Integer creditScore;
    @ApiModelProperty(value = "首发好物奖励是否已发放：0未发 1已发")
    private Integer firstAward;
}
