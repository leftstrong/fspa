package com.zbkj.service.service;


import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.shop.AdminCreateOrderRequest;
import com.zbkj.common.request.shop.AdminPreOrderRequest;
import com.zbkj.common.request.shop.AdminScanPaymentRequest;
import com.zbkj.common.request.shop.AdminStoreOrderSearchRequest;
import com.zbkj.common.response.StoreOrderCountItemResponse;
import com.zbkj.common.response.StoreOrderDetailResponse;
import com.zbkj.common.response.StoreOrderTopItemResponse;
import com.zbkj.common.response.shop.AdminProductDetailResponse;
import com.zbkj.common.vo.MyRecord;

/**
 * @author Chennong
 * @date 2023-05-10
 * @description 门店业务
 */
public interface ShopService {

    /**
     * 获取商品详情
     *
     * @param id   商品编号
     * @param type normal-正常，void-视频
     * @return 商品详情信息
     */
    AdminProductDetailResponse getDetail(Integer id, String type);

    /**
     * 门店预下单
     *
     * @param request
     * @return
     */
    MyRecord preOrder(AdminPreOrderRequest request);

    /**
     * 门店创建订单
     *
     * @param request
     * @return
     */
    MyRecord createOrder(AdminCreateOrderRequest request);

    /**
     * 门店订单列表
     *
     * @param request
     * @param pageParamRequest
     * @return
     */
    CommonPage<StoreOrderDetailResponse> getAdminList(AdminStoreOrderSearchRequest request, PageParamRequest pageParamRequest);

    /**
     * 获取订单状态数量
     *
     * @param dateLimit
     * @param type
     * @return
     */
    StoreOrderCountItemResponse getOrderStatusNum(String dateLimit, Integer type);

    /**
     * 获取订单统计数据
     *
     * @param dateLimit
     * @return
     */
    StoreOrderTopItemResponse getOrderData(String dateLimit);

    /**
     * @param phone
     * @return
     */
    User getAdminOrUser(String phone);

    /**
     * 扫码付款
     *
     * @param request
     * @return
     */
    boolean scanPayment(AdminScanPaymentRequest request) throws Exception;

    /**
     *  重新生成订单NO
     * @param oldOrderNo
     * @return
     */
    boolean resetOrderNo(String oldOrderNo);
}
