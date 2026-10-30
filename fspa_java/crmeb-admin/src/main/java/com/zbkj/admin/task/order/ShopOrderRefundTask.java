package com.zbkj.admin.task.order;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.constants.TaskConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.user.User;
import com.zbkj.common.utils.DateUtil;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.common.vo.MyRecord;
import com.zbkj.service.service.StoreOrderService;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.WeChatPayService;
import com.zbkj.service.service.YlyPrintService;
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
public class ShopOrderRefundTask {

    //日志
    private static final Logger logger = LoggerFactory.getLogger(ShopOrderRefundTask.class);

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

    @Scheduled(fixedDelay = 1000 * 60L) //6秒同步一次数据
    public void init() {
        logger.info("---ShopOrderRefundTask task------produce Data with fixed rate task: Execution Time - {}", DateUtil.nowDateTime());
        try {
            //查询已提交退款的门店订单 倒叙
            List<StoreOrder> shopOrderList = storeOrderService.list(Wrappers.<StoreOrder>lambdaQuery()
                    .eq(StoreOrder::getShippingType, 2)
                    .eq(StoreOrder::getIsDel, false)//未取消
                    .eq(StoreOrder::getRefundStatus, 3)//退款中
                    .eq(StoreOrder:: getIsChannel, 8)// 针对扫码支付的
                    .eq(StoreOrder::getPaid, 1).orderByDesc(StoreOrder::getId).last("limit 1"));
            if (CollectionUtils.isEmpty(shopOrderList)) {
                return;
            }
            //查询订单支付状态
            for (StoreOrder storeOrder : shopOrderList) {
                String queryResult = weChatPayService.microPayQuery(storeOrder.getOrderId());
                if (queryResult.contains("订单发生过退款，退款详情请查询退款单")) {
                    storeOrder.setRefundStatus(2);
                    boolean update = storeOrderService.updateById(storeOrder);
                    if (update) {
                        // 退款task
                        redisUtil.lPush(Constants.ORDER_TASK_REDIS_KEY_AFTER_REFUND_BY_USER, storeOrder.getId());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("ShopOrderRefundTask.task" + " | msg : " + e.getMessage());
        }

    }

}
