package com.zbkj.admin.task.order;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zbkj.common.constants.TaskConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.user.User;
import com.zbkj.common.utils.DateUtil;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.service.service.*;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * 门店订单支付后,状态查询task任务
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
@Component
@Configuration //读取配置
@EnableScheduling // 2.开启定时任务
public class ShopOrderPayTask {

    //日志
    private static final Logger logger = LoggerFactory.getLogger(ShopOrderPayTask.class);

    @Autowired
    private YlyPrintService ylyPrintService;
    @Autowired
    private StoreOrderService storeOrderService;
    @Autowired
    private WeChatPayService weChatPayService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private UserService userService;
    @Autowired
    private ShopService shopService;

    @Scheduled(fixedDelay = 1000 * 6L) //6秒同步一次数据
    public void init() {
        logger.info("---ShopOrderPayTask task------produce Data with fixed rate task: Execution Time - {}", DateUtil.nowDateTime());
        try {
            //查询未支付的门店订单 倒叙
            List<StoreOrder> shopOrderList = storeOrderService.list(Wrappers.<StoreOrder>lambdaQuery()
                    .eq(StoreOrder::getShippingType, 2)
                    .eq(StoreOrder::getIsDel, false)//未取消
                    .eq(StoreOrder::getRefundStatus, 0)//未退款
                    .eq(StoreOrder::getIsChannel, 8)// 针对扫码支付的
                    .eq(StoreOrder::getPaid, 0).orderByDesc(StoreOrder::getId).last("limit 1"));
            if (CollectionUtils.isEmpty(shopOrderList)) {
                return;
            }
            //查询订单支付状态
            for (StoreOrder storeOrder : shopOrderList) {
                // 用户余额扣除
                User user = userService.getById(storeOrder.getUid());
                if (ObjectUtil.isNull(user)) throw new CrmebException("用户不存在");
                String payResult = weChatPayService.microPayQuery(storeOrder.getOrderId());
                if (payResult.contains("支付成功")) {
                    storeOrder.setPaid(true);
                    storeOrder.setPayTime(DateUtil.nowDateTime());
                    Boolean execute = transactionTemplate.execute(e -> {
                        // 订单修改
                        storeOrderService.updateById(storeOrder);
                        // 扣除积分
                        if (storeOrder.getUseIntegral() > 0) {
                            userService.updateIntegral(user, storeOrder.getUseIntegral(), "sub");
                        }
                        // 成功 更新订单支付状态
                        storeOrderService.updateById(storeOrder);
                        // 添加支付成功redis队列
                        redisUtil.lPush(TaskConstants.ORDER_TASK_PAY_SUCCESS_AFTER, storeOrder.getOrderId());
                        // 打印小票
                        ylyPrintService.YlyPrint(storeOrder.getOrderId(), true);
                        return Boolean.TRUE;
                    });
                } else if (payResult.contains("请撤销订单")) {
                    //撤销订单
                    weChatPayService.microPayReverse(storeOrder.getOrderId());
                    //重新生成订单号
                    shopService.resetOrderNo(storeOrder.getOrderId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("ShopOrderPayTask.task" + " | msg : " + e.getMessage());
        }

    }

}
