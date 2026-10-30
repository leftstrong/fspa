package com.zbkj.service.service;

import com.zbkj.common.response.barter.BarterDashboardResponse;

import java.util.Map;

/**
 * 换物-数据驾驶舱 统计服务
 */
public interface BarterDashboardService {

    /** 汇总指标（总量 + 今日增量 + 派生指标） */
    BarterDashboardResponse summary();

    /**
     * 近 N 天趋势
     *
     * @param days 天数（含今天），建议 7 / 14 / 30
     * @return dates / user / item / order / message 五个等长数组
     */
    Map<String, Object> trend(int days);

    /**
     * 分布数据
     *
     * @return itemStatus / orderStatus / category 三组 [{name, value}]
     */
    Map<String, Object> distribute();
}
