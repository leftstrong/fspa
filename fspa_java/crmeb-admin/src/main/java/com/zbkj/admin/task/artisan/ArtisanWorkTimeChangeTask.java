package com.zbkj.admin.task.artisan;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.qcloud.cos.utils.Jackson;
import com.qcloud.cos.utils.StringUtils;
import com.zbkj.common.model.article.EbArtisanEntity;
import com.zbkj.common.utils.DateUtil;
import com.zbkj.service.service.EbArtisanService;
import com.zbkj.service.service.StoreBargainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 砍价活动结束状态变化定时任务
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
public class ArtisanWorkTimeChangeTask {

    //日志
    private static final Logger logger = LoggerFactory.getLogger(ArtisanWorkTimeChangeTask.class);

    @Autowired
    private EbArtisanService ebArtisanService;

    /**
     * 每天0点过后2分钟后执行
     */
    @Scheduled(cron = "2 0 0 * * ?")
    public void init() {
        logger.info("---ArtisanWorkTimeChangeTask------bargain stop status change task: Execution Time - {}", DateUtil.nowDateTime());
        try {
            List<EbArtisanEntity> list = ebArtisanService.list();
            if (CollectionUtils.isEmpty(list)) {
                return;
            }
            List<String> timeList = new ArrayList<>(Arrays.asList("10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00"));
            //昨天
            String yeasterDay = DateUtil.getMinusDays(1);
            //未来第7天
            String severDay = DateUtil.getPlusDays(6);
            for (EbArtisanEntity ebArtisan : list) {
                Map<String, List<String>> workTime = new HashMap<>();
                if (StringUtils.isNullOrEmpty(ebArtisan.getWorkTime())) {
                    workTime.put(severDay, timeList);
                } else {
                    workTime = Jackson.fromJsonString(ebArtisan.getWorkTime(), Map.class);
                    if (workTime.containsKey(yeasterDay)) {
                        workTime.remove(yeasterDay);
                    }
                    workTime.put(severDay, timeList);
                }
                ebArtisan.setWorkTime(Jackson.toJsonString(workTime));
            }
            ebArtisanService.saveOrUpdateBatch(list);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("ArtisanWorkTimeChangeTask" + " | msg : " + e.getMessage());
        }
    }

}
