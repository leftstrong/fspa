package com.zbkj.admin.task.barter;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.zbkj.common.constants.BarterConstants;
import com.zbkj.common.model.barter.EbBarterApply;
import com.zbkj.common.model.barter.EbBarterItem;
import com.zbkj.common.model.barter.EbBarterOrder;
import com.zbkj.service.dao.BarterApplyDao;
import com.zbkj.service.dao.BarterItemDao;
import com.zbkj.service.dao.BarterOrderDao;
import com.zbkj.service.service.BarterMessageService;
import com.zbkj.service.service.BarterNotifyService;
import com.zbkj.service.service.BarterOrderService;
import com.zbkj.service.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 以物换物 定时任务
 * - 关闭超时未确认的换物申请
 * - 自动完成超时未确认收货的换物单
 */
@Component
@Configuration
@EnableScheduling
public class BarterScheduleTask {

    private static final Logger logger = LoggerFactory.getLogger(BarterScheduleTask.class);

    @Autowired
    private BarterApplyDao applyDao;
    @Autowired
    private BarterOrderDao orderDao;
    @Autowired
    private BarterOrderService barterOrderService;
    @Autowired
    private BarterNotifyService barterNotifyService;
    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private BarterItemDao barterItemDao;
    @Autowired
    private BarterMessageService barterMessageService;

    private int getConfigInt(String key, int def) {
        try {
            String v = systemConfigService.getValueByKey(key);
            if (v != null && !v.trim().isEmpty()) return Integer.parseInt(v.trim());
        } catch (Exception ignore) {
        }
        return def;
    }

    /** 每 5 分钟执行一次 */
    @Scheduled(fixedDelay = 1000 * 60 * 5L)
    public void run() {
        logger.info("---BarterScheduleTask start------ {}", DateUtil.date());
        try {
            closeExpiredApply();
            autoCompleteExpiredOrder();
            expireTopItem();
        } catch (Exception e) {
            logger.error("BarterScheduleTask error: {}", e.getMessage());
        }
    }

    /**
     * 清理到期的置顶：把 top_end_time 置空，物品回到普通排序
     * - 列表排序本身已按 top_end_time > NOW() 判断，这里负责把过期数据真正"摘牌"，
     *   顺便给物主一条站内信，方便再次置顶
     * - 只对最近 24 小时内到期的发通知，避免部署首跑时把历史过期数据全部推一遍
     */
    private void expireTopItem() {
        Date now = new Date();
        QueryWrapper<EbBarterItem> qw = new QueryWrapper<>();
        qw.isNotNull("top_end_time").le("top_end_time", now);
        List<EbBarterItem> list = barterItemDao.selectList(qw);
        if (list == null || list.isEmpty()) return;
        Date justExpired = DateUtil.offsetHour(now, -24);
        int cleared = 0;
        for (EbBarterItem it : list) {
            // 带上旧值做条件，避免与"刚续期"的并发写打架
            UpdateWrapper<EbBarterItem> uw = new UpdateWrapper<>();
            uw.eq("id", it.getId()).eq("top_end_time", it.getTopEndTime());
            uw.set("top_end_time", null).set("update_time", now);
            int n = barterItemDao.update(null, uw);
            if (n <= 0) continue;
            cleared++;
            if (it.getTopEndTime().after(justExpired)) {
                String title = it.getTitle() != null ? it.getTitle() : "好物";
                try {
                    barterMessageService.push(it.getUid(), BarterConstants.MSG_SYSTEM, it.getId(),
                            "置顶已到期", "「" + title + "」的置顶已到期，已回到普通排序；需要继续推荐可以再次置顶");
                } catch (Exception ignore) {
                    // 消息失败不影响清理主流程
                }
            }
        }
        logger.info("BarterScheduleTask expireTopItem count={}", cleared);
    }

    /** 关闭超时未确认的换物申请（默认 48 小时） */
    private void closeExpiredApply() {
        int expireHour = getConfigInt("barter_apply_expire_hour", 48);
        Date deadline = DateUtil.offsetHour(new Date(), -expireHour);
        UpdateWrapper<EbBarterApply> uw = new UpdateWrapper<>();
        uw.eq("status", 1).lt("create_time", deadline);
        uw.set("status", 0).set("update_time", new Date());
        int n = applyDao.update(null, uw);
        if (n > 0) logger.info("BarterScheduleTask closeExpiredApply count={}", n);
    }

    /** 自动完成超时未确认收货的换物单（默认 7 天） */
    private void autoCompleteExpiredOrder() {
        int recvExpireHour = getConfigInt("barter_recv_expire_hour", 24 * 7);
        Date deadline = DateUtil.offsetHour(new Date(), -recvExpireHour);
        QueryWrapper<EbBarterOrder> qw = new QueryWrapper<>();
        qw.eq("status", 2).lt("update_time", deadline);
        List<EbBarterOrder> list = orderDao.selectList(qw);
        for (EbBarterOrder o : list) {
            barterOrderService.autoCompleteBySystem(o.getId());
            logger.info("BarterScheduleTask autoCompleteOrder id={}", o.getId());
        }
    }
}
