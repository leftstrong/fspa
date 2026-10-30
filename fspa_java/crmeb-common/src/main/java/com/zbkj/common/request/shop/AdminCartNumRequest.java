package com.zbkj.common.request.shop;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author Chennong
 * @date 2023-05-11
 * @description 购物车数量变更请求参数对象
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="AdminCartNumRequest对象", description="购物车数量变更请求参数对象")
public class AdminCartNumRequest implements Serializable {

    private static final long serialVersionUID = 3365523948381218093L;

    @ApiModelProperty("购物车ID")
    @NotNull(message = "购物车ID 不能为空")
    private Integer id;

    @ApiModelProperty("商品数量")
    @NotNull(message = "商品数量 不能为空")
    private Integer cartNum;

    @ApiModelProperty("商品属性ID")
    @NotNull(message = "商品属性ID 不能为空")
    private Integer productAttrId;
}
