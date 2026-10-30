package com.zbkj.common.response.shop;

import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttr;
import com.zbkj.common.response.ProductActivityItemResponse;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;


/**
 * @author Chennong
 * @date 2023-05-11
 * @description 商品详情
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="AdminProductDetailResponse对象", description="商品详情后台")
public class AdminProductDetailResponse implements Serializable {

    private static final long serialVersionUID = 1641682329537680983L;

    @ApiModelProperty(value = "产品属性")
    private List<StoreProductAttr> productAttr;

    @ApiModelProperty(value = "商品属性详情")
    private List<AdminProductDetailValue> productValue;

    @ApiModelProperty(value = "返佣金额区间")
    private String priceName;

    @ApiModelProperty(value = "为移动端特定参数 所有参与的活动")
    private List<ProductActivityItemResponse> activityAllH5;

    @ApiModelProperty(value = "商品信息")
    private StoreProduct productInfo;

    @ApiModelProperty(value = "收藏标识")
    private Boolean userCollect;
}
