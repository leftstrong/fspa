package com.zbkj.service.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.constants.*;
import com.zbkj.common.dict.PayChannelDict;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.bargain.StoreBargain;
import com.zbkj.common.model.bargain.StoreBargainUser;
import com.zbkj.common.model.cat.StoreCart;
import com.zbkj.common.model.combination.StoreCombination;
import com.zbkj.common.model.combination.StorePink;
import com.zbkj.common.model.coupon.StoreCouponUser;
import com.zbkj.common.model.express.ShippingTemplates;
import com.zbkj.common.model.express.ShippingTemplatesFree;
import com.zbkj.common.model.express.ShippingTemplatesRegion;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.order.StoreOrderInfo;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttr;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.model.record.UserVisitRecord;
import com.zbkj.common.model.seckill.StoreSeckill;
import com.zbkj.common.model.seckill.StoreSeckillManger;
import com.zbkj.common.model.system.*;
import com.zbkj.common.model.user.User;
import com.zbkj.common.model.user.UserAddress;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.OrderComputedPriceRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.shop.*;
import com.zbkj.common.response.*;
import com.zbkj.common.response.shop.AdminProductDetailResponse;
import com.zbkj.common.response.shop.AdminProductDetailValue;
import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.common.utils.SecurityUtil;
import com.zbkj.common.vo.*;
import com.zbkj.service.dao.StoreOrderDao;
import com.zbkj.service.dao.StoreProductDao;
import com.zbkj.service.delete.OrderUtils;
import com.zbkj.service.delete.ProductUtils;
import com.zbkj.service.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Chennong
 * @date 2023-05-10
 * @description 门店业务实现
 */
@Service
public class ShopServiceImpl implements ShopService {

    @Resource
    private StoreProductDao storeProductDao;

    @Autowired
    private StoreProductService storeProductService;

    @Autowired
    private UserService userService;

    @Autowired
    private StoreProductRelationService storeProductRelationService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private StoreProductAttrService attrService;

    @Autowired
    private StoreProductAttrValueService storeProductAttrValueService;

    @Autowired
    private SystemUserLevelService systemUserLevelService;

    @Autowired
    private UserVisitRecordService userVisitRecordService;

    @Autowired
    private ProductUtils productUtils;

    @Autowired
    private StoreProductAttrValueService attrValueService;

    @Autowired
    private StoreCartService storeCartService;

    @Autowired
    private StoreOrderInfoService storeOrderInfoService;

    @Autowired
    private StoreOrderService storeOrderService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private UserAddressService userAddressService;

    @Autowired
    private ShippingTemplatesFreeService shippingTemplatesFreeService;

    @Autowired
    private ShippingTemplatesRegionService shippingTemplatesRegionService;

    @Autowired
    private ShippingTemplatesService shippingTemplatesService;

    @Autowired
    private SystemNotificationService systemNotificationService;

    @Autowired
    private SmsTemplateService smsTemplateService;

    @Autowired
    private StoreOrderStatusService storeOrderStatusService;

    @Autowired
    private StoreProductReplyService storeProductReplyService;

    @Autowired
    private SystemStoreService systemStoreService;

    @Autowired
    private OrderUtils orderUtils;

    @Autowired
    private SystemAttachmentService systemAttachmentService;

    @Autowired
    private LogisticService logisticsService;

    @Autowired
    private StoreSeckillService storeSeckillService;

    @Autowired
    private StoreCombinationService storeCombinationService;

    @Autowired
    private StoreBargainService storeBargainService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private SystemAdminService systemAdminService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private SystemGroupDataService systemGroupDataService;

    @Autowired
    private StoreCouponUserService storeCouponUserService;

    @Autowired
    private StoreBargainUserService storeBargainUserService;

    @Autowired
    private StoreSeckillMangerService storeSeckillMangerService;

    @Autowired
    private StorePinkService storePinkService;

    @Resource
    private StoreOrderDao dao;

    @Autowired
    private SystemStoreStaffService systemStoreStaffService;

    @Autowired
    private WeChatPayService weChatPayService;

    /**
     * 获取商品详情
     *
     * @param id   商品编号
     * @param type normal-正常，video-视频
     * @return 商品详情信息
     */
    @Override
    public AdminProductDetailResponse getDetail(Integer id, String type) {
        // 获取用户
        User user = userService.getInfo();
        SystemUserLevel userLevel = null;
        if (ObjectUtil.isNotNull(user) && user.getLevel() > 0) {
            userLevel = systemUserLevelService.getByLevelId(user.getLevel());
        }

        AdminProductDetailResponse productDetailResponse = new AdminProductDetailResponse();
        // 查询商品
        StoreProduct storeProduct = storeProductService.getH5Detail(id);
        if (ObjectUtil.isNotNull(userLevel)) {
            storeProduct.setVipPrice(storeProduct.getPrice());
        }
        productDetailResponse.setProductInfo(storeProduct);

        // 获取商品规格
        List<StoreProductAttr> attrList = attrService.getListByProductIdAndType(storeProduct.getId(), Constants.PRODUCT_TYPE_NORMAL);
        // 根据制式设置attr属性
        productDetailResponse.setProductAttr(attrList);

        // 根据制式设置sku属性
        List<AdminProductDetailValue> productValueList = new ArrayList<>();
        List<StoreProductAttrValue> storeProductAttrValues = storeProductAttrValueService.getListByProductIdAndType(storeProduct.getId(), Constants.PRODUCT_TYPE_NORMAL);
        for (StoreProductAttrValue storeProductAttrValue : storeProductAttrValues) {
            AdminProductDetailValue detailValue = new AdminProductDetailValue();
            StoreProductAttrValueResponse atr = new StoreProductAttrValueResponse();
            BeanUtils.copyProperties(storeProductAttrValue, atr);
            // 设置会员价
            if (ObjectUtil.isNotNull(userLevel)) {
                atr.setVipPrice(atr.getPrice());
            }
            detailValue.setSukKey(atr.getSuk());
            detailValue.setValue(atr);
            productValueList.add(detailValue);
        }
        productDetailResponse.setProductValue(productValueList);

        // 用户收藏、分销返佣
        if (ObjectUtil.isNotNull(user)) {
            // 查询用户是否收藏收藏
            user = userService.getInfo();
            productDetailResponse.setUserCollect(storeProductRelationService.getLikeOrCollectByUser(user.getUid(), id, false).size() > 0);
            // 判断是否开启分销
            String brokerageFuncStatus = systemConfigService.getValueByKey(SysConfigConstants.CONFIG_KEY_BROKERAGE_FUNC_STATUS);
            if (brokerageFuncStatus.equals(Constants.COMMON_SWITCH_OPEN)) {// 分销开启
                // 判断是否开启气泡
                String isBubble = systemConfigService.getValueByKey(SysConfigConstants.CONFIG_KEY_STORE_BROKERAGE_IS_BUBBLE);
                if (isBubble.equals(Constants.COMMON_SWITCH_OPEN)) {
                    productDetailResponse.setPriceName(getPacketPriceRange(storeProduct.getIsSub(), storeProductAttrValues, user.getIsPromoter()));
                }
            }
        } else {
            productDetailResponse.setUserCollect(false);
        }
        // 商品活动
        List<ProductActivityItemResponse> activityAllH5 = productUtils.getProductAllActivity(storeProduct);
        productDetailResponse.setActivityAllH5(activityAllH5);

        // 商品浏览量+1
        StoreProduct updateProduct = new StoreProduct();
        updateProduct.setId(id);
        updateProduct.setBrowse(storeProduct.getBrowse() + 1);
        storeProductService.updateById(updateProduct);

        // 保存用户访问记录
        if (userService.getUserId() > 0) {
            UserVisitRecord visitRecord = new UserVisitRecord();
            visitRecord.setDate(DateUtil.date().toString("yyyy-MM-dd"));
            visitRecord.setUid(userService.getUserId());
            visitRecord.setVisitType(2);
            userVisitRecordService.save(visitRecord);
        }

        return productDetailResponse;
    }

    @Override
    public MyRecord preOrder(AdminPreOrderRequest request) {
        if (CollUtil.isEmpty(request.getOrderDetails())) {
            throw new CrmebException("预下单订单详情列表不能为空");
        }
        User user = getAdminOrUser(request.getPhone());
//        User user = userService.getInfoException();
        // 校验预下单商品信息
        OrderInfoVo orderInfoVo = validatePreOrderRequest(request, user);
        // 商品总计金额
        BigDecimal totalPrice;
        if (orderInfoVo.getOrderDetailList().get(0).getProductType().equals(Constants.PRODUCT_TYPE_NORMAL)) {
            // 普通商品
            totalPrice = orderInfoVo.getOrderDetailList().stream().map(e -> e.getVipPrice().multiply(new BigDecimal(e.getPayNum()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            totalPrice = orderInfoVo.getOrderDetailList().stream().map(e -> e.getPrice().multiply(new BigDecimal(e.getPayNum()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        orderInfoVo.setProTotalFee(totalPrice);
        // 购买商品总数量
        int orderProNum = orderInfoVo.getOrderDetailList().stream().mapToInt(OrderInfoDetailVo::getPayNum).sum();
        orderInfoVo.setOrderProNum(orderProNum);

        // 获取默认地址
        UserAddress userAddress = userAddressService.getDefaultByUid(user.getUid());
        if (ObjectUtil.isNotNull(userAddress)) {
            // 计算运费
            getFreightFee(orderInfoVo, userAddress);
            orderInfoVo.setAddressId(userAddress.getId());
            orderInfoVo.setRealName(userAddress.getRealName());
            orderInfoVo.setPhone(userAddress.getPhone());
            orderInfoVo.setProvince(userAddress.getProvince());
            orderInfoVo.setCity(userAddress.getCity());
            orderInfoVo.setDistrict(userAddress.getDistrict());
            orderInfoVo.setDetail(userAddress.getDetail());
        } else {
            orderInfoVo.setFreightFee(BigDecimal.ZERO);
        }
        // 实际支付金额
        orderInfoVo.setPayFee(orderInfoVo.getProTotalFee().add(orderInfoVo.getFreightFee()));
        orderInfoVo.setUserIntegral(user.getIntegral());
        orderInfoVo.setUserBalance(user.getNowMoney());
        // 缓存订单
        String key = user.getUid() + com.zbkj.common.utils.DateUtil.getNowTime().toString() + CrmebUtil.getUuid();
        redisUtil.set("user_order:" + key, JSONObject.toJSONString(orderInfoVo), Constants.ORDER_CASH_CONFIRM, TimeUnit.MINUTES);
        MyRecord record = new MyRecord();
        record.set("preOrderNo", key);
        return record;
    }

    @Override
    public User getAdminOrUser(String phone) {
        User user;
        SystemAdmin admin = SecurityUtil.getLoginUserVo().getUser();
        SystemStoreStaff temp = systemStoreStaffService.getOne(Wrappers.<SystemStoreStaff>lambdaQuery().eq(SystemStoreStaff::getUid, admin.getId()));
        Optional.ofNullable(temp).orElseThrow(() -> new CrmebException("当前账号未绑定门店"));
        if (StringUtils.isNotBlank(phone)) {
            user = userService.getByPhone(phone);
            Optional.ofNullable(user).orElseThrow(() -> new CrmebException("手机号错误,用户不存在"));
            user.setStoreId(temp.getStoreId());
        } else {
            user = userService.getOne(Wrappers.<User>lambdaQuery().eq(User::getStoreId, temp.getStoreId()));
            Optional.ofNullable(user).orElseThrow(() -> new CrmebException("当前门店未设置默认客户账号"));
        }
        return user;
    }

    @Override
    public boolean scanPayment(AdminScanPaymentRequest request) throws Exception {
        StoreOrder storeOrder = storeOrderService.getByOderId(request.getOrderNo());
        Optional.ofNullable(storeOrder).orElseThrow(() -> new CrmebException("该订单不存在"));
        SystemStore systemStore = systemStoreService.getById(storeOrder.getStoreId());
        weChatPayService.micropay(request.getAuthCode(),
                Optional.ofNullable(systemStore.getName()).orElse(""),
                request.getOrderNo(),
                storeOrder.getPayPrice().multiply(new BigDecimal(100)).intValue(),
                "192.168.0.1");
        storeOrder.setIsChannel(8);
        storeOrderService.updateById(storeOrder);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean resetOrderNo(String oldOrderNo) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getOrderId, oldOrderNo);
        StoreOrder storeOrder = dao.selectOne(lqw);
        if (ObjectUtil.isNull(storeOrder)) {
            return false;
        }
        String newOrderNo = CrmebUtil.getOrderNo("order");
        storeOrder.setOrderId(newOrderNo);
        dao.updateById(storeOrder);
        storeOrderInfoService.update(Wrappers.<StoreOrderInfo>lambdaUpdate()
                .eq(StoreOrderInfo::getOrderNo, oldOrderNo)
                .set(StoreOrderInfo::getOrderNo, newOrderNo));
        return true;
    }

    @Override
    public MyRecord createOrder(AdminCreateOrderRequest request) {
//        User user = userService.getInfoException();
        User user = getAdminOrUser(request.getPhone());
        // 通过缓存获取预下单对象
        String key = "user_order:" + request.getPreOrderNo();
        boolean exists = redisUtil.exists(key);
        if (!exists) {
            throw new CrmebException("预下单订单不存在");
        }
        String orderVoString = redisUtil.get(key).toString();
        OrderInfoVo orderInfoVo = JSONObject.parseObject(orderVoString, OrderInfoVo.class);

        // 检测支付方式
        if (!orderUtils.checkPayType(request.getPayType())) throw new CrmebException("暂不支持该支付方式，请刷新页面或者联系管理员");

        if (request.getPayType().equals(PayConstants.PAY_TYPE_WE_CHAT)) {
            // 检测支付渠道
            if (StrUtil.isBlank(request.getPayChannel())) throw new CrmebException("支付渠道不能为空!");
            if (!OrderUtils.checkPayChannel(request.getPayChannel())) throw new CrmebException("支付渠道不存在!");
        }
        // 校验商品库存
        List<MyRecord> skuRecordList = validateProductStock(orderInfoVo, user);
        // 校验收货信息
        String verifyCode = "";
        String userAddressStr = "";
//        if (request.getShippingType() == 1) { // 快递配送
//            if (request.getAddressId() <= 0) throw new CrmebException("请选择收货地址");
//            UserAddress userAddress = userAddressService.getById(request.getAddressId());
//            if (ObjectUtil.isNull(userAddress) || userAddress.getIsDel()) {
//                throw new CrmebException("收货地址有误");
//            }
//            request.setRealName(userAddress.getRealName());
//            request.setPhone(userAddress.getPhone());
//            userAddressStr = userAddress.getProvince() + userAddress.getCity() + userAddress.getDistrict() + userAddress.getDetail();
//        } else if (request.getShippingType() == 2) { // 到店自提
//            if (StringUtils.isBlank(request.getRealName()) || StringUtils.isBlank(request.getPhone())) {
//                throw new CrmebException("请填写姓名和电话");
//            }
        // 自提开关是否打开
//            String storeSelfMention = systemConfigService.getValueByKey(SysConfigConstants.CONFIG_KEY_STORE_SELF_MENTION);
//            if (storeSelfMention.equals("false")) {
//                throw new CrmebException("请先联系管理员开启门店自提");
//            }
//            SystemStore systemStore = systemStoreService.getById(user.getStoreId());
//            if (ObjectUtil.isNull(systemStore) || systemStore.getIsDel() || !systemStore.getIsShow()) {
//                throw new CrmebException("暂无门店无法选择门店自提");
//            }
        verifyCode = CrmebUtil.randomCount(1111111111, 999999999) + "";
//            userAddressStr = systemStore.getName();
//        }

        // 活动商品校验
//        // 秒杀
//        if (ObjectUtil.isNotNull(orderInfoVo.getSeckillId()) && orderInfoVo.getSeckillId() > 0) {
//            StoreSeckill storeSeckill = storeSeckillService.getByIdException(orderInfoVo.getSeckillId());
//            if (storeSeckill.getStatus().equals(0)) {
//                throw new CrmebException("秒杀商品已关闭");
//            }
//            OrderInfoDetailVo detailVo = orderInfoVo.getOrderDetailList().get(0);
//            StoreProductAttrValue seckillAttrValue = storeProductAttrValueService.getByIdAndProductIdAndType(detailVo.getAttrValueId(), orderInfoVo.getSeckillId(), Constants.PRODUCT_TYPE_SECKILL);
//            if (ObjectUtil.isNull(seckillAttrValue)) {
//                throw new CrmebException("秒杀商品规格不存在");
//            }
//            commonValidateSeckill(storeSeckill, seckillAttrValue, user, detailVo.getPayNum());
//        }

        // 计算订单各种价格
        OrderComputedPriceRequest orderComputedPriceRequest = new OrderComputedPriceRequest();
        orderComputedPriceRequest.setShippingType(request.getShippingType());
        orderComputedPriceRequest.setAddressId(request.getAddressId());
        orderComputedPriceRequest.setCouponId(request.getCouponId());
        orderComputedPriceRequest.setUseIntegral(request.getUseIntegral());
        ComputedOrderPriceResponse computedOrderPriceResponse = computedPrice(orderComputedPriceRequest, orderInfoVo, user);

        // 生成订单号
        String orderNo = CrmebUtil.getOrderNo("order");

        // 购买赠送的积分
        int gainIntegral = 0;
        List<StoreOrderInfo> storeOrderInfos = new ArrayList<>();
        for (OrderInfoDetailVo detailVo : orderInfoVo.getOrderDetailList()) {
            // 赠送积分
            if (ObjectUtil.isNotNull(detailVo.getGiveIntegral()) && detailVo.getGiveIntegral() > 0) {
                gainIntegral += detailVo.getGiveIntegral() * detailVo.getPayNum();
            }
            // 订单详情
            StoreOrderInfo soInfo = new StoreOrderInfo();
            soInfo.setProductId(detailVo.getProductId());
            soInfo.setInfo(JSON.toJSON(detailVo).toString());
            soInfo.setUnique(detailVo.getAttrValueId().toString());
            soInfo.setOrderNo(orderNo);
            soInfo.setProductName(detailVo.getProductName());
            soInfo.setAttrValueId(detailVo.getAttrValueId());
            soInfo.setImage(detailVo.getImage());
            soInfo.setSku(detailVo.getSku());
            soInfo.setPrice(detailVo.getPrice());
            soInfo.setPayNum(detailVo.getPayNum());
            soInfo.setWeight(detailVo.getWeight());
            soInfo.setVolume(detailVo.getVolume());
            if (ObjectUtil.isNotNull(detailVo.getGiveIntegral()) && detailVo.getGiveIntegral() > 0) {
                soInfo.setGiveIntegral(detailVo.getGiveIntegral());
            } else {
                soInfo.setGiveIntegral(0);
            }
            soInfo.setIsReply(false);
            soInfo.setIsSub(detailVo.getIsSub());
            soInfo.setProductType(detailVo.getProductType());
            if (ObjectUtil.isNotNull(detailVo.getVipPrice())) {
                soInfo.setVipPrice(detailVo.getVipPrice());
            } else {
                soInfo.setVipPrice(detailVo.getPrice());
            }
            storeOrderInfos.add(soInfo);
        }

        // 下单赠送积分
        if (computedOrderPriceResponse.getPayFee().compareTo(BigDecimal.ZERO) > 0) {
            // 赠送积分比例
            String integralStr = systemConfigService.getValueByKey(SysConfigConstants.CONFIG_KEY_INTEGRAL_RATE_ORDER_GIVE);
            if (StrUtil.isNotBlank(integralStr)) {
                BigDecimal integralBig = new BigDecimal(integralStr);
                int integral = integralBig.multiply(computedOrderPriceResponse.getPayFee()).setScale(0, BigDecimal.ROUND_DOWN).intValue();
                if (integral > 0) {
                    // 添加积分
                    gainIntegral += integral;
                }
            }
        }

        // 支付渠道
        int isChannel = PayChannelDict.get(request.getPayChannel());
        /*if (request.getPayType().equals(PayConstants.PAY_TYPE_WE_CHAT)) {
            switch (request.getPayChannel()) {
                case PayConstants.PAY_CHANNEL_WE_CHAT_H5:// H5
                    isChannel = 2;
                    break;
                case PayConstants.PAY_CHANNEL_WE_CHAT_PUBLIC:// 公众号
                    isChannel = 0;
                    break;
                case PayConstants.PAY_CHANNEL_WE_CHAT_PROGRAM:// 小程序
                    isChannel = 1;
                    break;
                case PayConstants.PAY_CHANNEL_WE_CHAT_APP_IOS:// app ios
                    isChannel = 4;
                    break;
                case PayConstants.PAY_CHANNEL_WE_CHAT_APP_ANDROID:// app android
                    isChannel = 5;
                    break;
                case PayConstants.PAY_CHANNEL_WE_CHAT_QRCODE_SCAN:// app android
                    isChannel = 8;
                    break;
            }
            isChannel = PayChannelDict.get(request.getPayType());
        }
        if (request.getPayType().equals(PayConstants.PAY_TYPE_ALI_PAY)) {
            isChannel = 6;
            if (request.getPayChannel().equals(PayConstants.PAY_CHANNEL_ALI_APP_PAY)) {
                isChannel = 7;
            }
        }*/

        StoreOrder storeOrder = new StoreOrder();
        storeOrder.setUid(user.getUid());
        storeOrder.setOrderId(orderNo);
        storeOrder.setRealName(request.getRealName());
        storeOrder.setUserPhone(request.getPhone());
        storeOrder.setUserAddress(userAddressStr);
        // 如果是自提
        if (request.getShippingType() == ShippingTypeConstants.SHOP) {
            storeOrder.setVerifyCode(verifyCode);
            storeOrder.setStoreId(request.getStoreId());
        }
        storeOrder.setTotalNum(orderInfoVo.getOrderProNum());
        storeOrder.setCouponId(Optional.ofNullable(request.getCouponId()).orElse(0));

        // 订单总价
        BigDecimal totalPrice = computedOrderPriceResponse.getProTotalFee().add(computedOrderPriceResponse.getFreightFee());

        storeOrder.setTotalPrice(totalPrice);
        storeOrder.setProTotalPrice(computedOrderPriceResponse.getProTotalFee());
        storeOrder.setTotalPostage(computedOrderPriceResponse.getFreightFee());
        storeOrder.setCouponPrice(computedOrderPriceResponse.getCouponFee());
        storeOrder.setPayPrice(computedOrderPriceResponse.getPayFee());
        storeOrder.setPayPostage(computedOrderPriceResponse.getFreightFee());
        storeOrder.setDeductionPrice(computedOrderPriceResponse.getDeductionPrice());
        storeOrder.setPayType(request.getPayType());
        storeOrder.setUseIntegral(computedOrderPriceResponse.getUsedIntegral());
        storeOrder.setGainIntegral(gainIntegral);
//        storeOrder.setMark(StringEscapeUtils.escapeHtml4(request.getMark()));
        storeOrder.setMark("");
        storeOrder.setStoreId(user.getStoreId());
        storeOrder.setCombinationId(orderInfoVo.getCombinationId());
        storeOrder.setPinkId(orderInfoVo.getPinkId());
        storeOrder.setSeckillId(orderInfoVo.getSeckillId());
        storeOrder.setBargainId(orderInfoVo.getBargainId());
        storeOrder.setBargainUserId(orderInfoVo.getBargainUserId());
        storeOrder.setCreateTime(com.zbkj.common.utils.DateUtil.nowDateTime());
        storeOrder.setShippingType(request.getShippingType());
        storeOrder.setIsChannel(isChannel);
        storeOrder.setPaid(false);
        storeOrder.setCost(BigDecimal.ZERO);
        storeOrder.setType(Constants.ORDER_TYPE_NORMAL);
        if (orderInfoVo.getIsVideo()) {
            storeOrder.setType(Constants.ORDER_TYPE_VIDEO);// 视频号订单
        }

        StoreCouponUser storeCouponUser = new StoreCouponUser();
        // 优惠券修改
        if (storeOrder.getCouponId() > 0) {
            storeCouponUser = storeCouponUserService.getById(storeOrder.getCouponId());
            storeCouponUser.setStatus(1);
        }
        StoreCouponUser finalStoreCouponUser = storeCouponUser;

        Boolean execute = transactionTemplate.execute(e -> {
            // 扣减库存
            // 需要根据是否活动商品，扣减不同的库存
            if (storeOrder.getSeckillId() > 0) {// 秒杀扣库存
                MyRecord skuRecord = skuRecordList.get(0);
                // 秒杀商品扣库存
                storeSeckillService.operationStock(skuRecord.getInt("activityId"), skuRecord.getInt("num"), "sub");
                // 秒杀商品规格扣库存
                storeProductAttrValueService.operationStock(skuRecord.getInt("activityAttrValueId"), skuRecord.getInt("num"), "sub", Constants.PRODUCT_TYPE_SECKILL);
                // 普通商品口库存
                storeProductService.operationStock(skuRecord.getInt("productId"), skuRecord.getInt("num"), "sub");
                // 普通商品规格扣库存
                storeProductAttrValueService.operationStock(skuRecord.getInt("attrValueId"), skuRecord.getInt("num"), "sub", Constants.PRODUCT_TYPE_NORMAL);
            } else if (storeOrder.getBargainId() > 0) {// 砍价扣库存
                MyRecord skuRecord = skuRecordList.get(0);
                // 砍价商品扣库存
                storeBargainService.operationStock(skuRecord.getInt("activityId"), skuRecord.getInt("num"), "sub");
                // 砍价商品规格扣库存
                storeProductAttrValueService.operationStock(skuRecord.getInt("activityAttrValueId"), skuRecord.getInt("num"), "sub", Constants.PRODUCT_TYPE_BARGAIN);
                // 普通商品口库存
                storeProductService.operationStock(skuRecord.getInt("productId"), skuRecord.getInt("num"), "sub");
                // 普通商品规格扣库存
                storeProductAttrValueService.operationStock(skuRecord.getInt("attrValueId"), skuRecord.getInt("num"), "sub", Constants.PRODUCT_TYPE_NORMAL);
            } else if (storeOrder.getCombinationId() > 0) {// 拼团扣库存
                MyRecord skuRecord = skuRecordList.get(0);
                // 拼团商品扣库存
                Boolean operationStock = storeCombinationService.operationStock(skuRecord.getInt("activityId"), skuRecord.getInt("num"), "sub");
                System.out.println("拼团商品扣库存operationStock " + operationStock);
                // 拼团商品规格扣库存
                storeProductAttrValueService.operationStock(skuRecord.getInt("activityAttrValueId"), skuRecord.getInt("num"), "sub", Constants.PRODUCT_TYPE_PINGTUAN);
                // 普通商品口库存
                storeProductService.operationStock(skuRecord.getInt("productId"), skuRecord.getInt("num"), "sub");
                // 普通商品规格扣库存
                storeProductAttrValueService.operationStock(skuRecord.getInt("attrValueId"), skuRecord.getInt("num"), "sub", Constants.PRODUCT_TYPE_NORMAL);
            } else { // 普通商品
                for (MyRecord skuRecord : skuRecordList) {
                    // 普通商品口库存
                    storeProductService.operationStock(skuRecord.getInt("productId"), skuRecord.getInt("num"), "sub");
                    // 普通商品规格扣库存
                    storeProductAttrValueService.operationStock(skuRecord.getInt("attrValueId"), skuRecord.getInt("num"), "sub", Constants.PRODUCT_TYPE_NORMAL);
                }
            }

            storeOrderService.create(storeOrder);
            storeOrderInfos.forEach(info -> info.setOrderId(storeOrder.getId()));
            // 优惠券修改
            if (storeOrder.getCouponId() > 0) {
                storeCouponUserService.updateById(finalStoreCouponUser);
            }
            // 保存购物车商品详情
            storeOrderInfoService.saveOrderInfos(storeOrderInfos);
            // 生成订单日志
            storeOrderStatusService.createLog(storeOrder.getId(), Constants.ORDER_STATUS_CACHE_CREATE_ORDER, "订单生成");

            // 清除购物车数据
            if (CollUtil.isNotEmpty(orderInfoVo.getCartIdList())) {
                storeCartService.deleteCartByIds(orderInfoVo.getCartIdList());
            }
            return Boolean.TRUE;
        });
        if (!execute) {
            throw new CrmebException("订单生成失败");
        }

        // 删除缓存订单
        if (redisUtil.exists(key)) {
            redisUtil.delete(key);
        }

        // 加入自动未支付自动取消队列
        redisUtil.lPush(Constants.ORDER_AUTO_CANCEL_KEY, storeOrder.getOrderId());

        // 发送后台管理员下单提醒通知短信
        sendAdminOrderNotice(storeOrder.getOrderId());

        MyRecord record = new MyRecord();
        record.set("orderNo", storeOrder.getOrderId());
        return record;
    }

    @Override
    public CommonPage<StoreOrderDetailResponse> getAdminList(AdminStoreOrderSearchRequest request, PageParamRequest pageParamRequest) {
        User user = this.getAdminOrUser(null);
        Page<Object> startPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "order_id", "uid", "real_name", "pay_price", "pay_type", "create_time", "status", "refund_status"
                , "refund_reason_wap_img", "refund_reason_wap_explain", "refund_reason_wap", "refund_reason", "refund_reason_time"
                , "is_del", "combination_id", "pink_id", "seckill_id", "bargain_id", "verify_code", "remark", "paid", "is_system_del", "shipping_type", "type", "is_alter_price");
        if (StrUtil.isNotBlank(request.getOrderNo())) {
            queryWrapper.eq("order_id", request.getOrderNo());
        }
        getRequestTimeWhere(queryWrapper, request);
        getStatusWhere(queryWrapper, request.getStatus());
        if (!request.getType().equals(2)) {
            queryWrapper.eq("type", request.getType());
        }
        queryWrapper.eq("store_id", user.getStoreId());
        queryWrapper.orderByDesc("id");
        List<StoreOrder> orderList = dao.selectList(queryWrapper);
        List<StoreOrderDetailResponse> detailResponseList = new ArrayList<>();
        if (CollUtil.isNotEmpty(orderList)) {
            detailResponseList = formatOrder1(orderList);
        }
        return CommonPage.restPage(CommonPage.copyPageInfo(startPage, detailResponseList));
    }

    /**
     * 获取订单状态数量
     *
     * @return StoreOrderCountItemResponse
     */
    @Override
    public StoreOrderCountItemResponse getOrderStatusNum(String dateLimit, Integer type) {
        StoreOrderCountItemResponse response = new StoreOrderCountItemResponse();
        if (type.equals(2)) {
            type = null;
        }
        // 全部订单
        response.setAll(getCount(dateLimit, Constants.ORDER_STATUS_ALL, type));
        // 未支付订单
        response.setUnPaid(getCount(dateLimit, Constants.ORDER_STATUS_UNPAID, type));
//        // 未发货订单
//        response.setNotShipped(getCount(dateLimit, Constants.ORDER_STATUS_NOT_SHIPPED, type));
//        // 待收货订单
//        response.setSpike(getCount(dateLimit, Constants.ORDER_STATUS_SPIKE, type));
        // 待评价订单
        response.setBargain(getCount(dateLimit, Constants.ORDER_STATUS_BARGAIN, type));
        // 交易完成订单
        response.setComplete(getCount(dateLimit, Constants.ORDER_STATUS_COMPLETE, type));
        // 待核销订单
        response.setToBeWrittenOff(getCount(dateLimit, Constants.ORDER_STATUS_TOBE_WRITTEN_OFF, type));
        // 退款中订单
        response.setRefunding(getCount(dateLimit, Constants.ORDER_STATUS_REFUNDING, type));
        // 已退款订单
        response.setRefunded(getCount(dateLimit, Constants.ORDER_STATUS_REFUNDED, type));
        // 已删除订单
        response.setDeleted(getCount(dateLimit, Constants.ORDER_STATUS_DELETED, type));
        return response;
    }

    @Override
    public StoreOrderTopItemResponse getOrderData(String dateLimit) {
        StoreOrderTopItemResponse itemResponse = new StoreOrderTopItemResponse();
        // 订单数量
        itemResponse.setCount(getCount(dateLimit, Constants.ORDER_STATUS_ALL));
        // 订单金额
        itemResponse.setAmount(getAmount(dateLimit, ""));
        // 微信支付金额
        itemResponse.setWeChatAmount(getAmount(dateLimit, Constants.PAY_TYPE_WE_CHAT));
        // 余额支付金额
        itemResponse.setYueAmount(getAmount(dateLimit, Constants.PAY_TYPE_YUE));
        return itemResponse;
    }

    /**
     * 获取订单总数
     *
     * @param dateLimit 时间端
     * @param status    String 状态
     * @return Integer
     */
    private Integer getCount(String dateLimit, String status) {
        //总数只计算时间
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(dateLimit)) {
            dateLimitUtilVo dateLimitUtilVo = com.zbkj.common.utils.DateUtil.getDateLimit(dateLimit);
            queryWrapper.between("create_time", dateLimitUtilVo.getStartTime(), dateLimitUtilVo.getEndTime());
        }
        getStatusWhereNew(queryWrapper, status);
        return dao.selectCount(queryWrapper);
    }

    /**
     * 获取订单金额
     *
     * @param dateLimit 时间端
     * @param type      支付类型
     * @return Integer
     */
    private BigDecimal getAmount(String dateLimit, String type) {
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("sum(pay_price) as pay_price");
        if (StringUtils.isNotBlank(type)) {
            queryWrapper.eq("pay_type", type);
        }
        queryWrapper.isNotNull("pay_time");
        queryWrapper.eq("paid", 1);
        if (StringUtils.isNotBlank(dateLimit)) {
            dateLimitUtilVo dateLimitUtilVo = com.zbkj.common.utils.DateUtil.getDateLimit(dateLimit);
            queryWrapper.between("create_time", dateLimitUtilVo.getStartTime(), dateLimitUtilVo.getEndTime());
        }
        StoreOrder storeOrder = dao.selectOne(queryWrapper);
        if (ObjectUtil.isNull(storeOrder)) {
            return BigDecimal.ZERO;
        }
        return storeOrder.getPayPrice();
    }

    /**
     * 获取订单总数
     *
     * @param dateLimit 时间端
     * @param status    String 状态
     * @return Integer
     */
    private Integer getCount(String dateLimit, String status, Integer type) {
        User user = this.getAdminOrUser(null);
        //总数只计算时间
        QueryWrapper<StoreOrder> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(dateLimit)) {
            dateLimitUtilVo dateLimitUtilVo = com.zbkj.common.utils.DateUtil.getDateLimit(dateLimit);
            queryWrapper.between("create_time", dateLimitUtilVo.getStartTime(), dateLimitUtilVo.getEndTime());
        }
        getStatusWhereNew(queryWrapper, status);
        if (ObjectUtil.isNotNull(type)) {
            queryWrapper.eq("type", type);
        }
        queryWrapper.eq("store_id", user.getStoreId());
        return dao.selectCount(queryWrapper);
    }

    /**
     * 根据订单状态获取where条件
     *
     * @param queryWrapper QueryWrapper<StoreOrder> 表达式
     * @param status       String 类型
     */
    private void getStatusWhereNew(QueryWrapper<StoreOrder> queryWrapper, String status) {
        if (StrUtil.isBlank(status)) {
            return;
        }
        switch (status) {
            case Constants.ORDER_STATUS_ALL: //全部
                break;
            case Constants.ORDER_STATUS_UNPAID: //未支付
                queryWrapper.eq("paid", 0);//支付状态
                queryWrapper.eq("status", 0); //订单状态
                queryWrapper.eq("is_del", 0);//删除状态
                break;
            case Constants.ORDER_STATUS_NOT_SHIPPED: //未发货
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 0);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("shipping_type", 1);//配送方式
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_SPIKE: //待收货
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 1);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_BARGAIN: //待评价
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 2);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_COMPLETE: //交易完成
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 3);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_TOBE_WRITTEN_OFF: //待核销
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 0);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("shipping_type", 2);//配送方式
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_REFUNDING: //退款中
                queryWrapper.eq("paid", 1);
                queryWrapper.in("refund_status", 1, 3);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_REFUNDED: //已退款
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("refund_status", 2);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_DELETED: //已删除
                queryWrapper.eq("is_del", 1);
                break;
            default:
                queryWrapper.eq("paid", 1);
                queryWrapper.ne("refund_status", 2);
                break;
        }
        queryWrapper.eq("is_system_del", 0);
    }

    /**
     * 格式化订单信息，对外输出一致
     *
     * @param orderList List<StoreOrder> 订单列表
     * @return List<StoreOrderItemResponse>
     */
    private List<StoreOrderDetailResponse> formatOrder1(List<StoreOrder> orderList) {
        List<StoreOrderDetailResponse> detailResponseList = new ArrayList<>();
        if (CollUtil.isEmpty(orderList)) {
            return detailResponseList;
        }

        //订单id集合
        List<Integer> orderIdList = orderList.stream().map(StoreOrder::getId).distinct().collect(Collectors.toList());

        //获取订单详情map
        HashMap<Integer, List<StoreOrderInfoOldVo>> orderInfoList = storeOrderInfoService.getMapInId(orderIdList);
//
//        //根据用户获取信息
//        List<Integer> userIdList = orderList.stream().map(StoreOrder::getUid).distinct().collect(Collectors.toList());
//        //订单用户信息
//        HashMap<Integer, User> userList = userService.getMapListInUid(userIdList);

        for (StoreOrder storeOrder : orderList) {
            StoreOrderDetailResponse storeOrderItemResponse = new StoreOrderDetailResponse();
            BeanUtils.copyProperties(storeOrder, storeOrderItemResponse);

            storeOrderItemResponse.setProductList(orderInfoList.get(storeOrder.getId()));

            //订单状态
            storeOrderItemResponse.setStatusStr(getStatus(storeOrder));
            storeOrderItemResponse.setStatus(storeOrder.getStatus());
            //支付方式
            storeOrderItemResponse.setPayTypeStr(getPayType(storeOrder.getPayType()));

            // 添加订单类型信息
            storeOrderItemResponse.setOrderType(getOrderTypeStr(storeOrder));
            detailResponseList.add(storeOrderItemResponse);
        }
        return detailResponseList;
    }

    /**
     * 获取订单类型（前端展示）
     *
     * @param storeOrder 订单
     * @return String
     */
    private String getOrderTypeStr(StoreOrder storeOrder) {
        String orderTypeFormat = "[{}订单]{}";
        String orderType = StrUtil.format(orderTypeFormat, "普通", "");
        // 核销
        if (StrUtil.isNotBlank(storeOrder.getVerifyCode())) {
            orderType = StrUtil.format(orderTypeFormat, "核销", "");
        }
        // 秒杀
        if (ObjectUtil.isNotNull(storeOrder.getSeckillId()) && storeOrder.getSeckillId() > 0) {
            orderType = StrUtil.format(orderTypeFormat, "秒杀", "");
        }
        // 砍价
        if (ObjectUtil.isNotNull(storeOrder.getBargainId()) && storeOrder.getBargainId() > 0) {
            orderType = StrUtil.format(orderTypeFormat, "砍价", "");
        }
        // 拼团
        if (ObjectUtil.isNotNull(storeOrder.getCombinationId()) && storeOrder.getCombinationId() > 0) {
            StorePink storePink = storePinkService.getById(storeOrder.getPinkId());
            if (ObjectUtil.isNotNull(storePink)) {
                String pinkstatus = "";
                if (storePink.getStatus() == 2) {
                    pinkstatus = "已完成";
                } else if (storePink.getStatus() == 3) {
                    pinkstatus = "未完成";
                } else {
                    pinkstatus = "正在进行中";
                }
                orderType = StrUtil.format(orderTypeFormat, "拼团", pinkstatus);
            }
        }
        if (storeOrder.getType().equals(Constants.ORDER_TYPE_VIDEO)) {// 视频订单
            orderType = StrUtil.format(orderTypeFormat, "视频号", "");
        }
        return orderType;
    }


    /**
     * 获取订单状态
     *
     * @param storeOrder StoreOrder 订单信息
     * @author Mr.Zhang
     * @since 2020-06-12
     */
    private Map<String, String> getStatus(StoreOrder storeOrder) {
        Map<String, String> map = new HashMap<>();
        map.put("key", "");
        map.put("value", "");
        if (null == storeOrder) {
            return map;
        }
        // 未支付
        if (!storeOrder.getPaid()
                && storeOrder.getStatus() == 0
                && storeOrder.getRefundStatus() == 0
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_UNPAID);
            map.put("value", Constants.ORDER_STATUS_STR_UNPAID);
            return map;
        }
        // 未发货
        if (storeOrder.getPaid()
                && storeOrder.getStatus() == 0
                && storeOrder.getRefundStatus() == 0
                && storeOrder.getShippingType() == 1
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_NOT_SHIPPED);
            map.put("value", Constants.ORDER_STATUS_STR_NOT_SHIPPED);
            return map;
        }
        // 待收货
        if (storeOrder.getPaid()
                && storeOrder.getStatus() == 1
                && storeOrder.getRefundStatus() == 0
                && storeOrder.getShippingType() == 1
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_SPIKE);
            map.put("value", Constants.ORDER_STATUS_STR_SPIKE);
            return map;
        }
        // 待评价
        if (storeOrder.getPaid()
                && storeOrder.getStatus() == 2
                && storeOrder.getRefundStatus() == 0
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_BARGAIN);
            map.put("value", Constants.ORDER_STATUS_STR_BARGAIN);
            return map;
        }
        // 交易完成
        if (storeOrder.getPaid()
                && storeOrder.getStatus() == 3
                && storeOrder.getRefundStatus() == 0
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_COMPLETE);
            map.put("value", Constants.ORDER_STATUS_STR_COMPLETE);
            return map;
        }
        // 待核销
        if (storeOrder.getPaid()
                && storeOrder.getStatus() == 0
                && storeOrder.getRefundStatus() == 0
                && storeOrder.getShippingType() == 2
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_TOBE_WRITTEN_OFF);
            map.put("value", Constants.ORDER_STATUS_STR_TOBE_WRITTEN_OFF);
            return map;
        }

        //申请退款
        if (storeOrder.getPaid()
                && storeOrder.getRefundStatus() == 1
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_APPLY_REFUNDING);
            map.put("value", Constants.ORDER_STATUS_STR_APPLY_REFUNDING);
            return map;
        }

        //退款中
        if (storeOrder.getPaid()
                && storeOrder.getRefundStatus() == 3
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_REFUNDING);
            map.put("value", Constants.ORDER_STATUS_STR_REFUNDING);
            return map;
        }

        //已退款
        if (storeOrder.getPaid()
                && storeOrder.getRefundStatus() == 2
                && !storeOrder.getIsDel()
                && !storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_REFUNDED);
            map.put("value", Constants.ORDER_STATUS_STR_REFUNDED);
        }

        //已删除
        if (storeOrder.getIsDel() || storeOrder.getIsSystemDel()) {
            map.put("key", Constants.ORDER_STATUS_DELETED);
            map.put("value", Constants.ORDER_STATUS_STR_DELETED);
        }

        return map;
    }

    /**
     * 获取支付文字
     *
     * @param payType String 支付方式
     */
    private String getPayType(String payType) {
        switch (payType) {
            case Constants.PAY_TYPE_WE_CHAT:
                return Constants.PAY_TYPE_STR_WE_CHAT;
            case Constants.PAY_TYPE_YUE:
                return Constants.PAY_TYPE_STR_YUE;
            case Constants.PAY_TYPE_ALI_PAY:
                return Constants.PAY_TYPE_STR_ALI_PAY;
            case Constants.PAY_TYPE_SCAN:
                return Constants.PAY_TYPE_STR_SCAN;
            default:
                return Constants.PAY_TYPE_STR_OTHER;
        }
    }

    /**
     * 获取request的where条件
     *
     * @param queryWrapper QueryWrapper<StoreOrder> 表达式
     * @param request      StoreOrderSearchRequest 请求参数
     */
    private void getRequestTimeWhere(QueryWrapper<StoreOrder> queryWrapper, AdminStoreOrderSearchRequest request) {
        if (StringUtils.isNotBlank(request.getDateLimit())) {
            dateLimitUtilVo dateLimitUtilVo = com.zbkj.common.utils.DateUtil.getDateLimit(request.getDateLimit());
            queryWrapper.between("create_time", dateLimitUtilVo.getStartTime(), dateLimitUtilVo.getEndTime());
        }
    }

    /**
     * 根据订单状态获取where条件
     *
     * @param queryWrapper QueryWrapper<StoreOrder> 表达式
     * @param status       String 类型
     */
    private void getStatusWhere(QueryWrapper<StoreOrder> queryWrapper, String status) {
        if (StrUtil.isBlank(status)) {
            return;
        }
        switch (status) {
            case Constants.ORDER_STATUS_ALL: //全部
                break;
            case Constants.ORDER_STATUS_UNPAID: //未支付
                queryWrapper.eq("paid", 0);//支付状态
                queryWrapper.eq("status", 0); //订单状态
                queryWrapper.eq("is_del", 0);//删除状态
                break;
            case Constants.ORDER_STATUS_NOT_SHIPPED: //未发货
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 0);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("shipping_type", 1);//配送方式
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_SPIKE: //待收货
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 1);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_BARGAIN: //待评价
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 2);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_COMPLETE: //交易完成
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 3);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_TOBE_WRITTEN_OFF: //待核销
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("status", 0);
                queryWrapper.eq("refund_status", 0);
                queryWrapper.eq("shipping_type", 2);//配送方式
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_REFUNDING: //退款中
                queryWrapper.eq("paid", 1);
                queryWrapper.in("refund_status", 1, 3);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_REFUNDED: //已退款
                queryWrapper.eq("paid", 1);
                queryWrapper.eq("refund_status", 2);
                queryWrapper.eq("is_del", 0);
                break;
            case Constants.ORDER_STATUS_DELETED: //已删除
                queryWrapper.eq("is_del", 1);
                break;
            default:
                queryWrapper.eq("paid", 1);
                queryWrapper.ne("refund_status", 2);
                break;
        }
        queryWrapper.eq("is_system_del", 0);
    }


    /**
     * 校验商品库存（生成订单）
     *
     * @param orderInfoVo 订单详情Vo
     * @return List<MyRecord>
     * skuRecord 扣减库存对象
     * ——activityId             活动商品id
     * ——activityAttrValueId    活动商品skuId
     * ——productId              普通（主）商品id
     * ——attrValueId            普通（主）商品skuId
     * ——num                    购买数量
     */
    private List<MyRecord> validateProductStock(OrderInfoVo orderInfoVo, User user) {
        List<MyRecord> recordList = CollUtil.newArrayList();
        if (orderInfoVo.getSeckillId() > 0) {
            // 秒杀部分判断
            Integer seckillId = orderInfoVo.getSeckillId();
            OrderInfoDetailVo detailVo = orderInfoVo.getOrderDetailList().get(0);
            StoreSeckill storeSeckill = storeSeckillService.getByIdException(seckillId);
            StoreProductAttrValue seckillAttrValue = storeProductAttrValueService.getByIdAndProductIdAndType(detailVo.getAttrValueId(), seckillId, Constants.PRODUCT_TYPE_SECKILL);
            if (ObjectUtil.isNull(seckillAttrValue)) {
                throw new CrmebException("秒杀商品规格不存在");
            }
            MyRecord seckillRecord = commonValidateSeckill(storeSeckill, seckillAttrValue, user, detailVo.getPayNum());
            // 主商品sku
            StoreProductAttrValue productAttrValue = seckillRecord.get("productAttrValue");

            MyRecord record = new MyRecord();
            record.set("activityId", seckillId);
            record.set("activityAttrValueId", seckillAttrValue.getId());
            record.set("productId", storeSeckill.getProductId());
            record.set("attrValueId", productAttrValue.getId());
            record.set("num", detailVo.getPayNum());
            recordList.add(record);
            return recordList;
        }
        if (orderInfoVo.getBargainId() > 0) {
            // 砍价部分判断
            Integer bargainId = orderInfoVo.getBargainId();
            OrderInfoDetailVo detailVo = orderInfoVo.getOrderDetailList().get(0);
            StoreBargain storeBargain = storeBargainService.getByIdException(bargainId);
            if (storeBargain.getStock().equals(0) || detailVo.getPayNum() > storeBargain.getStock()) {
                throw new CrmebException("砍价商品库存不足");
            }
            StoreProductAttrValue bargainAttrValue = storeProductAttrValueService.getByIdAndProductIdAndType(detailVo.getAttrValueId(), bargainId, Constants.PRODUCT_TYPE_BARGAIN);
            if (ObjectUtil.isNull(bargainAttrValue)) {
                throw new CrmebException("砍价商品规格不存在");
            }
            MyRecord bargainRecord = commonValidateBargain(storeBargain, bargainAttrValue, orderInfoVo.getBargainUserId(), user, detailVo.getPayNum());
            StoreProductAttrValue productAttrValue = bargainRecord.get("productAttrValue");

            MyRecord record = new MyRecord();
            record.set("activityId", bargainId);
            record.set("activityAttrValueId", bargainAttrValue.getId());
            record.set("productId", storeBargain.getProductId());
            record.set("attrValueId", productAttrValue.getId());
            record.set("num", detailVo.getPayNum());
            recordList.add(record);
            return recordList;
        }
        if (orderInfoVo.getCombinationId() > 0) {
            // 拼团部分判断
            Integer combinationId = orderInfoVo.getCombinationId();
            OrderInfoDetailVo detailVo = orderInfoVo.getOrderDetailList().get(0);
            StoreCombination storeCombination = storeCombinationService.getByIdException(combinationId);
            StoreProductAttrValue combinationAttrValue = storeProductAttrValueService.getByIdAndProductIdAndType(detailVo.getAttrValueId(), combinationId, Constants.PRODUCT_TYPE_PINGTUAN);
            if (ObjectUtil.isNull(combinationAttrValue)) {
                throw new CrmebException("拼团商品规格不存在");
            }
            MyRecord combinationRecord = commonValidateCombination(storeCombination, combinationAttrValue, user, detailVo.getPayNum());
            StoreProductAttrValue productAttrValue = combinationRecord.get("productAttrValue");

            MyRecord record = new MyRecord();
            record.set("activityId", combinationId);
            record.set("activityAttrValueId", combinationAttrValue.getId());
            record.set("productId", storeCombination.getProductId());
            record.set("attrValueId", productAttrValue.getId());
            record.set("num", detailVo.getPayNum());
            recordList.add(record);
            return recordList;
        }
        // 普通商品
        List<OrderInfoDetailVo> orderDetailList = orderInfoVo.getOrderDetailList();
        orderDetailList.forEach(e -> {
            // 查询商品信息
            StoreProduct storeProduct = storeProductService.getById(e.getProductId());
            if (ObjectUtil.isNull(storeProduct)) {
                throw new CrmebException("购买的商品信息不存在");
            }
            if (storeProduct.getIsDel()) {
                throw new CrmebException("购买的商品已删除");
            }
            if (!storeProduct.getIsShow()) {
                throw new CrmebException("购买的商品已下架");
            }
            if (storeProduct.getStock().equals(0) || e.getPayNum() > storeProduct.getStock()) {
                throw new CrmebException("购买的商品库存不足");
            }
            // 查询商品规格属性值信息
            StoreProductAttrValue attrValue = attrValueService.getByIdAndProductIdAndType(e.getAttrValueId(), e.getProductId(), Constants.PRODUCT_TYPE_NORMAL);
            if (ObjectUtil.isNull(attrValue)) {
                throw new CrmebException("购买的商品规格信息不存在");
            }
            if (attrValue.getStock() < e.getPayNum()) {
                throw new CrmebException("购买的商品库存不足");
            }
            MyRecord record = new MyRecord();
            record.set("productId", e.getProductId());
            record.set("num", e.getPayNum());
            record.set("attrValueId", e.getAttrValueId());
            recordList.add(record);
        });
        return recordList;
    }

    /**
     * 发送后台管理员下单提醒通知短信
     *
     * @param orderNo 订单编号
     */
    @Async
    public void sendAdminOrderNotice(String orderNo) {
        // 系统是否开启用户下单管理员提醒开关
        SystemNotification notification = systemNotificationService.getByMark(NotifyConstants.PLACE_AN_ORDER_ADMIN_MARK);
        if (!notification.getIsSms().equals(1)) {
            return;
        }
        // 查询可已发送短信的管理员
        List<SystemAdmin> systemAdminList = systemAdminService.findIsSmsList();
        if (CollUtil.isEmpty(systemAdminList)) {
            return;
        }
        // 发送短信
        systemAdminList.forEach(admin -> smsService.sendCreateOrderNotice(admin.getPhone(), orderNo, admin.getRealName(), null));
    }

    /**
     * 公共校验拼团
     *
     * @param storeCombination     砍价商品
     * @param combinationAttrValue 砍价商品规格属性
     * @param user                 用户
     * @param productNum           购买数量
     * @return MyRecord
     */
    private MyRecord commonValidateCombination(StoreCombination storeCombination, StoreProductAttrValue combinationAttrValue, User user, Integer productNum) {
        // 判断拼团时间段
        long timeMillis = System.currentTimeMillis();
        if (timeMillis < storeCombination.getStartTime()) {
            throw new CrmebException("拼团商品活动未开始");
        }
        if (timeMillis >= storeCombination.getStopTime()) {
            throw new CrmebException("拼团商品已过期");
        }
        // 判断购买数量
        if (productNum > storeCombination.getOnceNum()) {
            throw new CrmebException("购买数量超过单次拼团购买上限");
        }

        if (combinationAttrValue.getStock() <= 0 || combinationAttrValue.getQuota() <= 0 || productNum > combinationAttrValue.getStock()) {
            throw new CrmebException("拼团商品规格库存不足");
        }
        // 普通商品部分判断
        StoreProduct product = storeProductService.getById(storeCombination.getProductId());
        if (ObjectUtil.isNull(product) || product.getIsDel()) {
            throw new CrmebException("拼团主商品不存在");
        }
        if (product.getStock().equals(0) || productNum > product.getStock()) {
            throw new CrmebException("拼团主商品库存不足");
        }
        // 主商品sku
        StoreProductAttrValue productAttrValue = storeProductAttrValueService.getByProductIdAndSkuAndType(storeCombination.getProductId(), combinationAttrValue.getSuk(), Constants.PRODUCT_TYPE_NORMAL);
        if (ObjectUtil.isNull(productAttrValue)) {
            throw new CrmebException("拼团主商品规格不存在");
        }
        if (productAttrValue.getStock() <= 0 || productNum > productAttrValue.getStock()) {
            throw new CrmebException("拼团主商品规格库存不足");
        }

        // 用户参与活动的次数
        List<StoreOrder> userCombinationOrders = storeOrderService.getUserCurrentCombinationOrders(user.getUid(), storeCombination.getId());
        if (CollUtil.isNotEmpty(userCombinationOrders)) {
            // 判断是否有待支付订单
            List<StoreOrder> unPayOrders = userCombinationOrders.stream().filter(e -> !e.getPaid()).collect(Collectors.toList());
            if (unPayOrders.size() > 0) throw new CrmebException("您有拼团待支付订单，请支付后再购买");
            int payNum = userCombinationOrders.stream().mapToInt(StoreOrder::getTotalNum).sum();
            if (storeCombination.getNum() <= payNum) {
                throw new CrmebException("您已达到该商品拼团活动上限");
            }
            if ((payNum + productNum) > storeCombination.getNum()) {
                throw new CrmebException("超过该商品拼团活动您的购买上限");
            }
        }

        MyRecord record = new MyRecord();
        record.set("productAttrValue", productAttrValue);
        return record;
    }

    /**
     * 公共校验砍价
     *
     * @param storeBargain     砍价商品
     * @param bargainAttrValue 砍价商品规格属性
     * @param bargainUserId    砍价活动id
     * @param user             用户
     * @param productNum       购买数量
     * @return MyRecord
     */
    private MyRecord commonValidateBargain(StoreBargain storeBargain, StoreProductAttrValue bargainAttrValue, Integer bargainUserId, User user, Integer productNum) {
        if (storeBargain.getStock().equals(0) || productNum > storeBargain.getStock()) {
            throw new CrmebException("砍价商品库存不足");
        }
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis > storeBargain.getStopTime()) {
            throw new CrmebException("砍价活动已结束");
        }
        StoreBargainUser bargainUser = storeBargainUserService.getById(bargainUserId);
        if (ObjectUtil.isNull(bargainUser)) {
            throw new CrmebException("用户砍价活动不存在");
        }
        if (bargainUser.getIsDel()) {
            throw new CrmebException("用户砍价活动已取消");
        }
        if (!bargainUser.getStatus().equals(3)) {
            if (bargainUser.getBargainPriceMin().compareTo(bargainUser.getBargainPrice().subtract(bargainUser.getPrice())) != 0) {
                throw new CrmebException("请先完成砍价活动");
            }
        }
        // 判断是否有订单
        StoreOrder bargainOrder = storeOrderService.getByBargainOrder(bargainUser.getBargainId(), bargainUser.getId());
        if (ObjectUtil.isNotNull(bargainOrder)) {
            if (!bargainOrder.getPaid()) {
                throw new CrmebException("订单已创建，尚未支付");
            }
            throw new CrmebException("该砍价活动已创建了订单");
        }
        // 参与活动次数 -根据用户和秒杀信息查询当天订单判断订单数量
        List<StoreOrder> userCurrentBargainOrders = storeOrderService.getUserCurrentBargainOrders(user.getUid(), storeBargain.getId());
        if (CollUtil.isNotEmpty(userCurrentBargainOrders)) {
            // 判断是否有待支付订单
            List<StoreOrder> unPayOrders = userCurrentBargainOrders.stream().filter(e -> !e.getPaid()).collect(Collectors.toList());
            if (unPayOrders.size() > 0) throw new CrmebException("您有砍价待支付订单，请支付后再购买");

            // 判断是否达到上限
            if (CollUtil.isNotEmpty(userCurrentBargainOrders) && userCurrentBargainOrders.size() >= storeBargain.getNum()) {
                throw new CrmebException("您已经达到当前砍价活动上限");
            }
        }

        if (bargainAttrValue.getStock() <= 0 || bargainAttrValue.getQuota() <= 0 || productNum > bargainAttrValue.getStock()) {
            throw new CrmebException("砍价商品规格库存不足");
        }
        // 普通商品部分判断
        StoreProduct product = storeProductService.getById(storeBargain.getProductId());
        if (ObjectUtil.isNull(product) || product.getIsDel()) {
            throw new CrmebException("砍价主商品不存在");
        }
        if (product.getStock().equals(0) || productNum > product.getStock()) {
            throw new CrmebException("砍价主商品库存不足");
        }
        // 主商品sku
        StoreProductAttrValue productAttrValue = storeProductAttrValueService.getByProductIdAndSkuAndType(storeBargain.getProductId(), bargainAttrValue.getSuk(), Constants.PRODUCT_TYPE_NORMAL);
        if (ObjectUtil.isNull(productAttrValue)) {
            throw new CrmebException("砍价主商品规格不存在");
        }
        if (productAttrValue.getStock() <= 0 || productNum > productAttrValue.getStock()) {
            throw new CrmebException("砍价主商品规格库存不足");
        }

        MyRecord record = new MyRecord();
        record.set("productAttrValue", productAttrValue);
        return record;
    }

    /**
     * 公共校验秒杀
     *
     * @param storeSeckill     秒杀商品
     * @param seckillAttrValue 秒杀商品规格属性
     * @param user             用户
     * @return MyRecord
     */
    private MyRecord commonValidateSeckill(StoreSeckill storeSeckill, StoreProductAttrValue seckillAttrValue, User user, Integer productNum) {
        if (storeSeckill.getStock().equals(0) || productNum > storeSeckill.getStock()) {
            throw new CrmebException("秒杀商品库存不足");
        }
        if (seckillAttrValue.getStock() <= 0 || seckillAttrValue.getQuota() <= 0 || productNum > seckillAttrValue.getStock()) {
            throw new CrmebException("秒杀商品规格库存不足");
        }
        // 普通商品部分判断
        StoreProduct product = storeProductService.getById(storeSeckill.getProductId());
        if (ObjectUtil.isNull(product) || product.getIsDel()) {
            throw new CrmebException("秒杀主商品不存在");
        }
        if (product.getStock().equals(0) || productNum > product.getStock()) {
            throw new CrmebException("秒杀主商品库存不足");
        }
        // 主商品sku
        StoreProductAttrValue productAttrValue = storeProductAttrValueService.getByProductIdAndSkuAndType(storeSeckill.getProductId(), seckillAttrValue.getSuk(), Constants.PRODUCT_TYPE_NORMAL);
        if (ObjectUtil.isNull(productAttrValue)) {
            throw new CrmebException("秒杀主商品规格不存在");
        }
        if (productAttrValue.getStock() <= 0 || productNum > productAttrValue.getStock()) {
            throw new CrmebException("秒杀主商品规格库存不足");
        }

        // 判断秒杀是否过期 1:日期是否在范围内 2：时间是否在每天的时间段内
        StoreSeckillManger seckillManger = storeSeckillMangerService.getById(storeSeckill.getTimeId());
        if (ObjectUtil.isNull(seckillManger)) {
            throw new CrmebException("秒杀时段不存在");
        }
        // 判断日期是否过期
        DateTime nowDateTime = cn.hutool.core.date.DateUtil.date();
        String stopTimeStr = com.zbkj.common.utils.DateUtil.dateToStr(storeSeckill.getStopTime(), Constants.DATE_FORMAT_DATE);
        Date stopDate = com.zbkj.common.utils.DateUtil.strToDate(stopTimeStr + " " + seckillManger.getEndTime() + ":00:00", Constants.DATE_FORMAT);
        if (nowDateTime.getTime() - stopDate.getTime() >= 0) {
            throw new CrmebException("秒杀商品已过期");
        }
        // 判断是否在秒杀时段内（小时）,秒杀开始时间 <= 当前时间 <= 秒杀结束时间
        int hour = nowDateTime.getField(Calendar.HOUR_OF_DAY);// 现在的小时
        if (seckillManger.getStartTime() > hour || seckillManger.getEndTime() < hour) {
            throw new CrmebException("秒杀商品已过期");
        }

        // 判断秒杀购买次数
        // 当天参与活动次数 -根据用户和秒杀信息查询当天订单判断订单数量
        List<StoreOrder> userCurrentDaySecKillOrders = storeOrderService.getUserCurrentDaySecKillOrders(user.getUid(), storeSeckill.getId());
        if (CollUtil.isNotEmpty(userCurrentDaySecKillOrders)) {
            // 判断是否有待支付订单
            List<StoreOrder> unPayOrders = userCurrentDaySecKillOrders.stream().filter(e -> !e.getPaid()).collect(Collectors.toList());
            if (unPayOrders.size() > 0) {
                throw new CrmebException("您有秒杀待支付订单，请支付后再购买");
            }

            // 判断是否达到上限
            if (userCurrentDaySecKillOrders.size() >= storeSeckill.getNum()) {
                throw new CrmebException("您已经达到当前秒杀活动上限");
            }
        }

        MyRecord record = new MyRecord();
        record.set("productAttrValue", productAttrValue);
        return record;
    }

    private ComputedOrderPriceResponse computedPrice(OrderComputedPriceRequest request, OrderInfoVo orderInfoVo, User user) {
        // 计算各种价格
        ComputedOrderPriceResponse priceResponse = new ComputedOrderPriceResponse();
        // 计算运费
        if (request.getShippingType().equals(2)) {// 到店自提，不计算运费
            priceResponse.setFreightFee(BigDecimal.ZERO);
        } else if (ObjectUtil.isNull(request.getAddressId()) || request.getAddressId() <= 0) {
            // 快递配送，无地址
            priceResponse.setFreightFee(BigDecimal.ZERO);
        } else {// 快递配送，有地址
            UserAddress userAddress = userAddressService.getById(request.getAddressId());
            if (ObjectUtil.isNull(userAddress)) {
                priceResponse.setFreightFee(BigDecimal.ZERO);
            } else {
                getFreightFee(orderInfoVo, userAddress);
                priceResponse.setFreightFee(orderInfoVo.getFreightFee());
            }
        }
        // 计算优惠券金额
        if (ObjectUtil.isNull(request.getCouponId()) || request.getCouponId() <= 0) {
            priceResponse.setCouponFee(BigDecimal.ZERO);
        } else if (orderInfoVo.getSeckillId() > 0 || orderInfoVo.getBargainId() > 0 || orderInfoVo.getCombinationId() > 0) {
            throw new CrmebException("营销活动商品无法使用优惠券");
        } else if (orderInfoVo.getIsVideo()) {
            throw new CrmebException("视频号商品无法使用优惠券");
        } else {
            // 判断优惠券是否可以使用
            StoreCouponUser storeCouponUser = storeCouponUserService.getById(request.getCouponId());
            if (ObjectUtil.isNull(storeCouponUser) || !storeCouponUser.getUid().equals(user.getUid())) {
                throw new CrmebException("优惠券领取记录不存在！");
            }
            if (storeCouponUser.getStatus() == 1) {
                throw new CrmebException("此优惠券已使用！");
            }

            if (storeCouponUser.getStatus() == 2) {
                throw new CrmebException("此优惠券已失效！");
            }
            //判断是否在使用时间内
            Date date = com.zbkj.common.utils.DateUtil.nowDateTime();
            if (storeCouponUser.getStartTime().compareTo(date) > 0) {
                throw new CrmebException("此优惠券还未到达使用时间范围之内！");
            }
            if (date.compareTo(storeCouponUser.getEndTime()) > 0) {
                throw new CrmebException("此优惠券已经失效了");
            }
            if (storeCouponUser.getMinPrice().compareTo(orderInfoVo.getProTotalFee()) > 0) {
                throw new CrmebException("总金额小于优惠券最小使用金额");
            }
            //检测优惠券信息
            if (storeCouponUser.getUseType() > 1) {
                List<Integer> productIdList = orderInfoVo.getOrderDetailList().stream().map(OrderInfoDetailVo::getProductId).collect(Collectors.toList());
                if (productIdList.size() < 1) {
                    throw new CrmebException("没有找到商品");
                }

                //设置优惠券所提供的集合
                List<Integer> primaryKeyIdList = CrmebUtil.stringToArray(storeCouponUser.getPrimaryKey());

                //取两个集合的交集，如果是false则证明没有相同的值
                //oldList.retainAll(newList)返回值代表oldList是否保持原样，如果old和new完全相同，那old保持原样并返回false。
                //交集：listA.retainAll(listB) ——listA内容变为listA和listB都存在的对象；listB不变
                if (storeCouponUser.getUseType() == 2) {
                    primaryKeyIdList.retainAll(productIdList);
                    if (CollUtil.isEmpty(primaryKeyIdList)) {
                        throw new CrmebException("此优惠券为商品券，请购买相关商品之后再使用！");
                    }
                }

                if (storeCouponUser.getUseType() == 3) {
                    //拿出需要使用优惠券的商品分类集合
                    List<Integer> categoryIdList = storeProductService.getSecondaryCategoryByProductId(StringUtils.join(productIdList, ","));

                    primaryKeyIdList.retainAll(categoryIdList);
                    if (CollUtil.isEmpty(primaryKeyIdList)) {
                        throw new CrmebException("此优惠券为分类券，请购买相关分类下的商品之后再使用！");
                    }
                }
            }
            if (orderInfoVo.getProTotalFee().compareTo(storeCouponUser.getMoney()) <= 0) {
                priceResponse.setCouponFee(orderInfoVo.getProTotalFee());
                priceResponse.setDeductionPrice(BigDecimal.ZERO);
                priceResponse.setSurplusIntegral(user.getIntegral());
                priceResponse.setPayFee(priceResponse.getFreightFee());
                priceResponse.setUsedIntegral(0);
                priceResponse.setUseIntegral(false);
                priceResponse.setProTotalFee(orderInfoVo.getProTotalFee());
                return priceResponse;
            } else {
                priceResponse.setCouponFee(storeCouponUser.getMoney());
            }
        }
        // 积分部分
        BigDecimal payPrice = orderInfoVo.getProTotalFee().add(priceResponse.getFreightFee()).subtract(priceResponse.getCouponFee());
        priceResponse.setUseIntegral(request.getUseIntegral());
        priceResponse.setProTotalFee(orderInfoVo.getProTotalFee());
        if (!request.getUseIntegral() || user.getIntegral() <= 0) {// 不使用积分
            priceResponse.setDeductionPrice(BigDecimal.ZERO);
            priceResponse.setSurplusIntegral(user.getIntegral());
            priceResponse.setPayFee(payPrice);
            priceResponse.setUsedIntegral(0);
            return priceResponse;
        }
        // 使用积分
        // 查询积分使用比例
        String integralRatio = systemConfigService.getValueByKey(SysConfigConstants.CONFIG_KEY_INTEGRAL_RATE);
        BigDecimal deductionPrice = new BigDecimal(user.getIntegral()).multiply(new BigDecimal(integralRatio));
        if (request.getUseIntegral()) {
            // 积分兑换金额小于实际支付金额
            if (deductionPrice.compareTo(payPrice) < 0) {
                payPrice = payPrice.subtract(deductionPrice);
                priceResponse.setSurplusIntegral(0);
                priceResponse.setUsedIntegral(user.getIntegral());
            } else {
                deductionPrice = payPrice;
                if (payPrice.compareTo(BigDecimal.ZERO) > 0) {
                    int usedIntegral = payPrice.divide(new BigDecimal(integralRatio), 0, BigDecimal.ROUND_UP).intValue();
                    priceResponse.setSurplusIntegral(user.getIntegral() - usedIntegral);
                    priceResponse.setUsedIntegral(usedIntegral);
                }
                payPrice = BigDecimal.ZERO;
            }
            priceResponse.setPayFee(payPrice);
            priceResponse.setDeductionPrice(deductionPrice);
        }
        return priceResponse;
    }

    /**
     * 计算订单运费
     */
    private void getFreightFee(OrderInfoVo orderInfoVo, UserAddress userAddress) {
        // 判断是否满额包邮 type=1按件数 2按重量 3按体积
        // 全场满额包邮开关
        String postageSwitchString = systemConfigService.getValueByKey(SysConfigConstants.STORE_FEE_POSTAGE_SWITCH);
        // 全场满额包邮金额
        String storeFreePostageString = systemConfigService.getValueByKey(SysConfigConstants.STORE_FEE_POSTAGE);
        BigDecimal storePostage = BigDecimal.ZERO;
        if (postageSwitchString.equals("true") && (storeFreePostageString.equals("0") || orderInfoVo.getProTotalFee().compareTo(new BigDecimal(storeFreePostageString)) >= 0)) {
            storePostage = BigDecimal.ZERO;
        } else if (ObjectUtil.isNull(userAddress) || userAddress.getCityId() <= 0) {
            // 用户地址不存在，默认运费为0元
            storePostage = BigDecimal.ZERO;
        } else {
            // 有用户地址的情况下
            // 运费根据商品计算
            Map<Integer, MyRecord> proMap = CollUtil.newHashMap();
            orderInfoVo.getOrderDetailList().forEach(e -> {
                Integer proId = e.getProductId();
                if (proMap.containsKey(proId)) {
                    MyRecord record = proMap.get(proId);
                    record.set("totalPrice", record.getBigDecimal("totalPrice").add(e.getPrice().multiply(BigDecimal.valueOf(e.getPayNum()))));
                    record.set("totalNum", record.getInt("totalNum") + e.getPayNum());
                    BigDecimal weight = e.getWeight().multiply(BigDecimal.valueOf(e.getPayNum()));
                    record.set("weight", record.getBigDecimal("weight").add(weight));
                    BigDecimal volume = e.getVolume().multiply(BigDecimal.valueOf(e.getPayNum()));
                    record.set("volume", record.getBigDecimal("volume").add(volume));
                } else {
                    MyRecord record = new MyRecord();
                    record.set("totalPrice", e.getPrice().multiply(BigDecimal.valueOf(e.getPayNum())));
                    record.set("totalNum", e.getPayNum());
                    record.set("tempId", e.getTempId());
                    record.set("proId", proId);
                    BigDecimal weight = e.getWeight().multiply(BigDecimal.valueOf(e.getPayNum()));
                    record.set("weight", weight);
                    BigDecimal volume = e.getVolume().multiply(BigDecimal.valueOf(e.getPayNum()));
                    record.set("volume", volume);

                    proMap.put(proId, record);
                }
            });

            // 指定包邮（单品运费模板）> 指定区域配送（单品运费模板）
            int cityId = userAddress.getCityId();

            for (Map.Entry<Integer, MyRecord> m : proMap.entrySet()) {
                MyRecord value = m.getValue();
                Integer tempId = value.getInt("tempId");
                ShippingTemplates shippingTemplate = shippingTemplatesService.getById(tempId);
                if (shippingTemplate.getAppoint()) {// 指定包邮
                    // 判断是否在指定包邮区域内
                    // 必须满足件数 + 金额 才能包邮
                    ShippingTemplatesFree shippingTemplatesFree = shippingTemplatesFreeService.getByTempIdAndCityId(tempId, cityId);
                    if (ObjectUtil.isNotNull(shippingTemplatesFree)) { // 在包邮区域内
                        BigDecimal freeNum = shippingTemplatesFree.getNumber();
                        BigDecimal multiply = value.getBigDecimal("totalPrice");
                        if (new BigDecimal(value.getInt("totalNum")).compareTo(freeNum) >= 0 && multiply.compareTo(shippingTemplatesFree.getPrice()) >= 0) {
                            // 满足件数 + 金额 = 包邮
                            continue;
                        }
                    }
                }
                // 不满足指定包邮条件，走指定区域配送
                ShippingTemplatesRegion shippingTemplatesRegion = shippingTemplatesRegionService.getByTempIdAndCityId(tempId, cityId);
                if (ObjectUtil.isNull(shippingTemplatesRegion)) {
                    throw new CrmebException("计算运费时，未找到全国运费配置");
                }

                // 判断计费方式：件数、重量、体积
                switch (shippingTemplate.getType()) {
                    case 1: // 件数
                        // 判断件数是否超过首件
                        Integer num = value.getInt("totalNum");
                        if (num <= shippingTemplatesRegion.getFirst().intValue()) {
                            storePostage = storePostage.add(shippingTemplatesRegion.getFirstPrice());
                        } else {// 超过首件的需要计算续件
                            int renewalNum = num - shippingTemplatesRegion.getFirst().intValue();
                            // 剩余件数/续件 = 需要计算的续件费用的次数
                            BigDecimal divide = new BigDecimal(renewalNum).divide(shippingTemplatesRegion.getRenewal(), 0, BigDecimal.ROUND_UP);
                            BigDecimal renewalPrice = shippingTemplatesRegion.getRenewalPrice().multiply(divide);
                            storePostage = storePostage.add(shippingTemplatesRegion.getFirstPrice()).add(renewalPrice);
                        }
                        break;
                    case 2: // 重量
                        BigDecimal weight = value.getBigDecimal("weight");
                        if (weight.compareTo(shippingTemplatesRegion.getFirst()) <= 0) {
                            storePostage = storePostage.add(shippingTemplatesRegion.getFirstPrice());
                        } else {// 超过首件的需要计算续件
                            BigDecimal renewalNum = weight.subtract(shippingTemplatesRegion.getFirst());
                            // 剩余件数/续件 = 需要计算的续件费用的次数
                            BigDecimal divide = renewalNum.divide(shippingTemplatesRegion.getRenewal(), 0, BigDecimal.ROUND_UP);
                            BigDecimal renewalPrice = shippingTemplatesRegion.getRenewalPrice().multiply(divide);
                            storePostage = storePostage.add(shippingTemplatesRegion.getFirstPrice()).add(renewalPrice);
                        }
                        break;
                    case 3: // 体积
                        BigDecimal volume = value.getBigDecimal("volume");
                        if (volume.compareTo(shippingTemplatesRegion.getFirst()) <= 0) {
                            storePostage = storePostage.add(shippingTemplatesRegion.getFirstPrice());
                        } else {// 超过首件的需要计算续件
                            BigDecimal renewalNum = volume.subtract(shippingTemplatesRegion.getFirst());
                            // 剩余件数/续件 = 需要计算的续件费用的次数
                            BigDecimal divide = renewalNum.divide(shippingTemplatesRegion.getRenewal(), 0, BigDecimal.ROUND_UP);
                            BigDecimal renewalPrice = shippingTemplatesRegion.getRenewalPrice().multiply(divide);
                            storePostage = storePostage.add(shippingTemplatesRegion.getFirstPrice()).add(renewalPrice);
                        }
                        break;
                }
            }
        }
        orderInfoVo.setFreightFee(storePostage);
    }

    /**
     * 校验预下单商品信息
     *
     * @param request 预下单请求参数
     * @return OrderInfoVo
     */
    private OrderInfoVo validatePreOrderRequest(AdminPreOrderRequest request, User user) {
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        List<OrderInfoDetailVo> detailVoList = CollUtil.newArrayList();
        if (request.getPreOrderType().equals("shoppingCart")) {// 购物车购买
            detailVoList = validatePreOrderShopping(request, user);
            List<Long> cartIdList = request.getOrderDetails().stream().map(AdminPreOrderDetailRequest::getShoppingCartId).distinct().collect(Collectors.toList());
            orderInfoVo.setCartIdList(cartIdList);
        }
        if (request.getPreOrderType().equals("again")) {// 再次购买
            AdminPreOrderDetailRequest detailRequest = request.getOrderDetails().get(0);
            detailVoList = validatePreOrderAgain(detailRequest, user);
        }
        orderInfoVo.setOrderDetailList(detailVoList);
        return orderInfoVo;
    }

    /**
     * 再次下单预下单校验
     *
     * @param detailRequest 请求参数
     * @return List<OrderInfoDetailVo>
     */
    private List<OrderInfoDetailVo> validatePreOrderAgain(AdminPreOrderDetailRequest detailRequest, User user) {
        List<OrderInfoDetailVo> detailVoList = CollUtil.newArrayList();
        if (StrUtil.isBlank(detailRequest.getOrderNo())) {
            throw new CrmebException("再次购买订单编号不能为空");
        }
        StoreOrder storeOrder = getByOrderIdException(detailRequest.getOrderNo());
        if (storeOrder.getRefundStatus() > 0 || storeOrder.getStatus() != 3) {
            throw new CrmebException("只有已完成状态订单才能再次购买");
        }
        if (storeOrder.getSeckillId() > 0 || storeOrder.getBargainId() > 0 || storeOrder.getCombinationId() > 0) {
            throw new CrmebException("活动商品订单不能再次购买");
        }
        if (storeOrder.getType().equals(Constants.ORDER_TYPE_VIDEO)) {
            throw new CrmebException("视频订单不能再次购买");
        }
        // 获取订单详情
        List<StoreOrderInfoVo> infoVoList = storeOrderInfoService.getVoListByOrderId(storeOrder.getId());
        if (CollUtil.isEmpty(infoVoList)) {
            throw new CrmebException("订单详情未找到");
        }
        SystemUserLevel userLevel = null;
        if (user.getLevel() > 0) {
            userLevel = systemUserLevelService.getByLevelId(user.getLevel());
        }
        SystemUserLevel finalUserLevel = userLevel;
        infoVoList.forEach(e -> {
            OrderInfoDetailVo detailVo = e.getInfo();
            // 查询商品信息
            StoreProduct storeProduct = storeProductService.getById(detailVo.getProductId());
            if (ObjectUtil.isNull(storeProduct)) {
                throw new CrmebException("商品信息不存在，请刷新后重新选择");
            }
            if (storeProduct.getIsDel()) {
                throw new CrmebException("商品已删除，请刷新后重新选择");
            }
            if (!storeProduct.getIsShow()) {
                throw new CrmebException("商品已下架，请刷新后重新选择");
            }
            if (storeProduct.getStock() < detailVo.getPayNum()) {
                throw new CrmebException("商品库存不足，请刷新后重新选择");
            }
            // 查询商品规格属性值信息
            StoreProductAttrValue attrValue = attrValueService.getByIdAndProductIdAndType(detailVo.getAttrValueId(), detailVo.getProductId(), Constants.PRODUCT_TYPE_NORMAL);
            if (ObjectUtil.isNull(attrValue)) {
                throw new CrmebException("商品规格信息不存在，请刷新后重新选择");
            }
            if (attrValue.getStock() < detailVo.getPayNum()) {
                throw new CrmebException("商品规格库存不足，请刷新后重新选择");
            }
            OrderInfoDetailVo tempDetailVo = new OrderInfoDetailVo();
            tempDetailVo.setProductId(storeProduct.getId());
            tempDetailVo.setProductName(storeProduct.getStoreName());
            tempDetailVo.setAttrValueId(attrValue.getId());
            tempDetailVo.setSku(attrValue.getSuk());
            tempDetailVo.setPrice(attrValue.getPrice());
            tempDetailVo.setPayNum(detailVo.getPayNum());
            tempDetailVo.setImage(StrUtil.isNotBlank(attrValue.getImage()) ? attrValue.getImage() : storeProduct.getImage());
            tempDetailVo.setVolume(attrValue.getVolume());
            tempDetailVo.setWeight(attrValue.getWeight());
            tempDetailVo.setTempId(storeProduct.getTempId());
            tempDetailVo.setGiveIntegral(storeProduct.getGiveIntegral());
            tempDetailVo.setIsSub(storeProduct.getIsSub());
            tempDetailVo.setProductType(Constants.PRODUCT_TYPE_NORMAL);
            tempDetailVo.setVipPrice(attrValue.getPrice());
            if (ObjectUtil.isNotNull(finalUserLevel)) {
                tempDetailVo.setVipPrice(attrValue.getPrice());
            }
            detailVoList.add(tempDetailVo);
        });
        return detailVoList;
    }

    private StoreOrder getByOrderIdException(String orderId) {
        StoreOrder storeOrder = storeOrderService.getByOderId(orderId);
        if (ObjectUtil.isNull(storeOrder)) {
            throw new CrmebException("订单不存在");
        }
        if (storeOrder.getIsDel() || storeOrder.getIsSystemDel()) {
            throw new CrmebException("订单不存在");
        }
        return storeOrder;
    }

    /**
     * 购物车预下单校验
     *
     * @param request 请求参数
     * @param user    用户
     * @return List<OrderInfoDetailVo>
     */
    private List<OrderInfoDetailVo> validatePreOrderShopping(AdminPreOrderRequest request, User user) {
        List<OrderInfoDetailVo> detailVoList = CollUtil.newArrayList();
        SystemUserLevel userLevel = null;
        if (user.getLevel() > 0) {
            userLevel = systemUserLevelService.getByLevelId(user.getLevel());
        }
        SystemUserLevel finalUserLevel = userLevel;
        User shopUser = this.getAdminOrUser(null);
        request.getOrderDetails().forEach(e -> {
            if (ObjectUtil.isNull(e.getShoppingCartId())) {
                throw new CrmebException("购物车编号不能为空");
            }
            StoreCart storeCart = storeCartService.getByIdAndUid(e.getShoppingCartId(), shopUser.getUid());
            if (ObjectUtil.isNull(storeCart)) {
                throw new CrmebException("未找到对应的购物车信息");
            }
            // 查询商品信息
            StoreProduct storeProduct = storeProductService.getById(storeCart.getProductId());
            if (ObjectUtil.isNull(storeProduct)) {
                throw new CrmebException("商品信息不存在，请刷新后重新选择");
            }
            if (storeProduct.getIsDel()) {
                throw new CrmebException("商品已删除，请刷新后重新选择");
            }
            if (!storeProduct.getIsShow()) {
                throw new CrmebException("商品已下架，请刷新后重新选择");
            }
            if (storeProduct.getStock() < storeCart.getCartNum()) {
                throw new CrmebException("商品库存不足，请刷新后重新选择");
            }
            // 查询商品规格属性值信息
            StoreProductAttrValue attrValue = attrValueService.getByIdAndProductIdAndType(Integer.valueOf(storeCart.getProductAttrUnique()), storeCart.getProductId(), Constants.PRODUCT_TYPE_NORMAL);
            if (ObjectUtil.isNull(attrValue)) {
                throw new CrmebException("商品规格信息不存在，请刷新后重新选择");
            }
            if (attrValue.getStock() < storeCart.getCartNum()) {
                throw new CrmebException("商品规格库存不足，请刷新后重新选择");
            }
            OrderInfoDetailVo detailVo = new OrderInfoDetailVo();
            detailVo.setProductId(storeProduct.getId());
            detailVo.setProductName(storeProduct.getStoreName());
            detailVo.setAttrValueId(attrValue.getId());
            detailVo.setSku(attrValue.getSuk());
            detailVo.setPrice(attrValue.getPrice());
            detailVo.setPayNum(storeCart.getCartNum());
            detailVo.setImage(StrUtil.isNotBlank(attrValue.getImage()) ? attrValue.getImage() : storeProduct.getImage());
            detailVo.setVolume(attrValue.getVolume());
            detailVo.setWeight(attrValue.getWeight());
            detailVo.setTempId(storeProduct.getTempId());
            detailVo.setGiveIntegral(storeProduct.getGiveIntegral());
            detailVo.setIsSub(storeProduct.getIsSub());
            detailVo.setProductType(Constants.PRODUCT_TYPE_NORMAL);
            detailVo.setVipPrice(detailVo.getPrice());
            if (ObjectUtil.isNotNull(finalUserLevel)) {
                BigDecimal vipPrice = detailVo.getPrice().multiply(new BigDecimal(finalUserLevel.getDiscount())).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
                detailVo.setVipPrice(vipPrice);
            }
            detailVoList.add(detailVo);
        });
        return detailVoList;
    }

    /**
     * 获取商品佣金区间
     *
     * @param isSub         是否单独计算分佣
     * @param attrValueList 商品属性列表
     * @param isPromoter    是否推荐人
     * @return String 金额区间
     */
    private String getPacketPriceRange(Boolean isSub, List<StoreProductAttrValue> attrValueList, Boolean isPromoter) {
        String priceName = "0";
        if (!isPromoter) return priceName;
        // 获取一级返佣比例
        String brokerageRatioString = systemConfigService.getValueByKey(SysConfigConstants.CONFIG_KEY_STORE_BROKERAGE_RATIO);
        BigDecimal BrokerRatio = new BigDecimal(brokerageRatioString).divide(new BigDecimal("100"), 2, RoundingMode.DOWN);
        BigDecimal maxPrice;
        BigDecimal minPrice;
        // 获取佣金比例区间
        if (isSub) { // 是否单独分拥
            maxPrice = attrValueList.stream().map(StoreProductAttrValue::getBrokerage).reduce(BigDecimal.ZERO, BigDecimal::max);
            minPrice = attrValueList.stream().map(StoreProductAttrValue::getBrokerage).reduce(BigDecimal.ZERO, BigDecimal::min);
        } else {
            BigDecimal _maxPrice = attrValueList.stream().map(StoreProductAttrValue::getPrice).reduce(BigDecimal.ZERO, BigDecimal::max);
            BigDecimal _minPrice = attrValueList.stream().map(StoreProductAttrValue::getPrice).reduce(BigDecimal.ZERO, BigDecimal::min);
            maxPrice = BrokerRatio.multiply(_maxPrice).setScale(2, RoundingMode.HALF_UP);
            minPrice = BrokerRatio.multiply(_minPrice).setScale(2, RoundingMode.HALF_UP);
        }
        if (minPrice.compareTo(BigDecimal.ZERO) == 0 && maxPrice.compareTo(BigDecimal.ZERO) == 0) {
            priceName = "0";
        } else if (minPrice.compareTo(BigDecimal.ZERO) == 0 && maxPrice.compareTo(BigDecimal.ZERO) > 0) {
            priceName = maxPrice.toString();
        } else if (minPrice.compareTo(BigDecimal.ZERO) > 0 && maxPrice.compareTo(BigDecimal.ZERO) > 0) {
            priceName = minPrice.toString();
        } else if (minPrice.compareTo(maxPrice) == 0) {
            priceName = minPrice.toString();
        } else {
            priceName = minPrice.toString() + "~" + maxPrice.toString();
        }
        return priceName;
    }

}
