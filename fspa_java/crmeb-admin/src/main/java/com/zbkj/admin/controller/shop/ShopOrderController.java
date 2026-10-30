package com.zbkj.admin.controller.shop;

import com.zbkj.common.constants.PayConstants;
import com.zbkj.common.constants.PreOrderTypeConstants;
import com.zbkj.common.constants.ShippingTypeConstants;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.shop.AdminCreateOrderRequest;
import com.zbkj.common.request.shop.AdminPreOrderRequest;
import com.zbkj.common.request.shop.AdminScanPaymentRequest;
import com.zbkj.common.request.shop.AdminStoreOrderSearchRequest;
import com.zbkj.common.response.CommonResult;
import com.zbkj.common.response.StoreOrderCountItemResponse;
import com.zbkj.common.response.StoreOrderDetailResponse;
import com.zbkj.common.response.StoreOrderTopItemResponse;
import com.zbkj.common.vo.MyRecord;
import com.zbkj.service.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author Chennong
 * @date 2023-05-12
 * @description 门店订单接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/shop/order")
@Api(tags = "门店接口")
public class ShopOrderController {

    @Autowired
    private ShopService shopService;

    /**
     * 预下单
     */
    @ApiOperation(value = "预下单")
    @RequestMapping(value = "/pre", method = RequestMethod.POST)
    public CommonResult<Map<String, Object>> preOrder(@RequestBody @Validated AdminPreOrderRequest request) {
        return CommonResult.success(shopService.preOrder(request));
    }

    /**
     * 创建订单
     */
    @ApiOperation(value = "创建订单")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public CommonResult<Map<String, Object>> createOrder(@Validated @RequestBody AdminCreateOrderRequest orderRequest) {
        AdminPreOrderRequest preOrderRequest = orderRequest.getPreOrderRequest();
        preOrderRequest.setPreOrderType(PreOrderTypeConstants.SHOPPING_CART);
        preOrderRequest.setPhone(orderRequest.getPhone());
        MyRecord myRecord = shopService.preOrder(preOrderRequest);
        Object preOrderNo = myRecord.get("preOrderNo");
        orderRequest.setUseIntegral(false);
        orderRequest.setPreOrderNo(preOrderNo.toString());
        orderRequest.setPayType(PayConstants.PAY_TYPE_WE_CHAT);
        orderRequest.setPayChannel(PayConstants.PAY_CHANNEL_WE_CHAT_QRCODE_SCAN);
        orderRequest.setShippingType(ShippingTypeConstants.SHOP);
        return CommonResult.success(shopService.createOrder(orderRequest));
    }

    /**
     * 分页显示订单表
     *
     * @param request          搜索条件
     * @param pageParamRequest 分页参数
     */
    @PreAuthorize("hasAuthority('admin:order:list')")
    @ApiOperation(value = "分页列表") //配合swagger使用
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<StoreOrderDetailResponse>> getList(@Validated AdminStoreOrderSearchRequest request, @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(shopService.getAdminList(request, pageParamRequest));
    }

    /**
     * 获取订单各状态数量
     */
    @PreAuthorize("hasAuthority('admin:order:status:num')")
    @ApiOperation(value = "获取订单各状态数量")
    @RequestMapping(value = "/status/num", method = RequestMethod.GET)
    public CommonResult<StoreOrderCountItemResponse> getOrderStatusNum(
            @RequestParam(value = "dateLimit", defaultValue = "") String dateLimit,
            @RequestParam(value = "type", defaultValue = "3") @Range(min = 0, max = 3, message = "未知的订单类型") Integer type) {
        return CommonResult.success(shopService.getOrderStatusNum(dateLimit, type));
    }

    /**
     * 获取订单统计数据
     */
    @PreAuthorize("hasAuthority('admin:order:list:data')")
    @ApiOperation(value = "获取订单统计数据")
    @RequestMapping(value = "/list/data", method = RequestMethod.GET)
    public CommonResult<StoreOrderTopItemResponse> getOrderData(@RequestParam(value = "dateLimit", defaultValue = "") String dateLimit) {
        return CommonResult.success(shopService.getOrderData(dateLimit));
    }

    /**
     * 扫码付款
     */
    @ApiOperation(value = "扫码付款")
    @PostMapping(value = "/scanPayment")
    public CommonResult<String> scanPayment(@Validated @RequestBody AdminScanPaymentRequest request) throws Exception {
        boolean result = shopService.scanPayment(request);
        return CommonResult.success(result ? "支付成功" : "支付失败");
    }
}
