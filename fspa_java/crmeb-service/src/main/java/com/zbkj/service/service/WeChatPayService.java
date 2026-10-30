package com.zbkj.service.service;

import com.zbkj.common.model.finance.UserRecharge;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 微信支付
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2022 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 */
public interface WeChatPayService {

    /**
     * 查询支付结果
     * @param orderNo 订单编号
     * @return
     */
    Boolean queryPayResult(String orderNo);

    /**
     * 微信充值预下单接口
     * @param userRecharge 充值订单
     * @param clientIp      ip
     * @return 获取wechat.requestPayment()参数
     */
    Map<String, String> unifiedRecharge(UserRecharge userRecharge, String clientIp);

    /**
     * 微信支付被扫支付接口
     * @param authCode
     * @param body
     * @param outTradeNo
     * @param totalFee
     * @param spbillCreateIp
     * @return
     */
    String micropay(String authCode, String body, String outTradeNo, int totalFee, String spbillCreateIp) throws Exception;

    /**
     * 微信支付被扫订单查询接口
     * @param outTradeNo
     * @return
     * @throws Exception
     */
    String microPayQuery(String outTradeNo) throws Exception;

    /**
     * 微信支付被扫码支付退款接口
     * @param outTradeNo
     * @param outRefundNo
     * @param totalFee
     * @param refundFee
     * @return
     * @throws Exception
     */
    String scanPayRefund(String outTradeNo, String outRefundNo, int totalFee, int refundFee) throws Exception;

    /**
     * 微信支付被扫码支付撤销支付订单接口
     * @param outTradeNo
     * @return
     * @throws Exception
     */
    String microPayReverse(String outTradeNo) throws Exception;
}
