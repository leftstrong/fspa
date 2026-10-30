package com.zbkj.common.response.shop;

import com.zbkj.common.response.StoreProductAttrValueResponse;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * @author Chennong
 * @date 2023-05-11
 * @description 商品属性
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "AdminProductDetailValue对象", description = "商品详情H5")
public class AdminProductDetailValue implements Serializable {

    private static final long serialVersionUID = -9173546254304777093L;

    private String sukKey;

    private StoreProductAttrValueResponse value;
}
