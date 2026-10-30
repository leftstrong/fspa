package com.zbkj.common.request.shop;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author Chennong
 * @date 2023-05-12
 * @description 门店预下单详情请求对象
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="AdminPreOrderRequest对象", description="门店预下单详情请求对象")
public class AdminPreOrderDetailRequest implements Serializable {

    private static final long serialVersionUID = -3121127872451073609L;

    @ApiModelProperty(value = "购物车编号，购物车预下单时必填")
    private Long shoppingCartId;

    @ApiModelProperty(value = "商品id（立即购买必填）")
    private Integer productId;

    @ApiModelProperty(value = "商品规格属性id（立即购买、活动购买必填）")
    private Integer attrValueId;

    @ApiModelProperty(value = "商品数量（立即购买、活动购买必填）")
    private Integer productNum;

    @ApiModelProperty(value = "订单编号（再次购买必填）")
    private String orderNo;

    @ApiModelProperty(value = "砍价商品id")
    private Integer bargainId = 0;

    @ApiModelProperty(value = "用户砍价活动id")
    private Integer bargainUserId = 0;

    @ApiModelProperty(value = "拼团商品id")
    private Integer combinationId = 0;

    @ApiModelProperty(value = "拼团团长id")
    private Integer pinkId = 0;

    @ApiModelProperty(value = "秒杀商品id")
    private Integer seckillId = 0;
}
