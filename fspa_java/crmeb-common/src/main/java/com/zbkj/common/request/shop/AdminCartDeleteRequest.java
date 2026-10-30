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
@ApiModel(value="AdminCartDeleteRequest对象", description="购物车数量变更请求参数对象")
public class AdminCartDeleteRequest implements Serializable {

    private static final long serialVersionUID = 4101140808935609319L;

    @ApiModelProperty("购物车ID")
    @NotNull(message = "购物车ID 不能为空")
    private Long id;
}
