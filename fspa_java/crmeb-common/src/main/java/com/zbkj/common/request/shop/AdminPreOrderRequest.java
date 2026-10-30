package com.zbkj.common.request.shop;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * @author Chennong
 * @date 2023-05-12
 * @description 门店预下单请求参数对象
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "AdminPreOrderRequest对象", description = "门店预下单请求对象")
public class AdminPreOrderRequest implements Serializable {

    private static final long serialVersionUID = -799915799788738037L;

    @ApiModelProperty(value = "预下单类型（“shoppingCart”：购物车下单，“buyNow”：立即购买，”again“： 再次购买，”video“: 视频号商品下单）")
    private String preOrderType;

    @ApiModelProperty(value = "用户手机号")
    private String phone;

    @ApiModelProperty(value = "订单详情列表")
    private List<AdminPreOrderDetailRequest> orderDetails;
}
