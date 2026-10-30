package com.zbkj.common.request.shop;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @author Chennong
 * @date 2023-05-13
 * @description 扫码付款请求对象
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "AdminScanPaymentRequest对象", description = "扫码付款请求对象")
public class AdminScanPaymentRequest implements Serializable {
    private static final long serialVersionUID = 2299635262204772286L;

    @ApiModelProperty(value = "订单号")
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @ApiModelProperty(value = "付款码")
    @NotBlank(message = "付款码不能为空")
    private String authCode;
}
