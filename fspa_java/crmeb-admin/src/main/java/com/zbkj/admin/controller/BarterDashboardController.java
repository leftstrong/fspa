package com.zbkj.admin.controller;

import com.zbkj.common.response.CommonResult;
import com.zbkj.common.response.barter.BarterDashboardResponse;
import com.zbkj.service.service.BarterDashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 换物-数据驾驶舱 控制器
 */
@RestController
@RequestMapping("api/admin/barter/dashboard")
@Api(tags = "换物-数据驾驶舱")
public class BarterDashboardController {

    @Autowired
    private BarterDashboardService barterDashboardService;

    @ApiOperation(value = "汇总指标（总量 / 今日增量 / 派生指标）")
    @RequestMapping(value = "/summary", method = RequestMethod.GET)
    public CommonResult<BarterDashboardResponse> summary() {
        return CommonResult.success(barterDashboardService.summary());
    }

    @ApiOperation(value = "近 N 天趋势（用户/物品/换物单/留言）")
    @RequestMapping(value = "/trend", method = RequestMethod.GET)
    public CommonResult<Map<String, Object>> trend(@RequestParam(required = false, defaultValue = "14") Integer days) {
        return CommonResult.success(barterDashboardService.trend(days == null ? 14 : days));
    }

    @ApiOperation(value = "分布（物品状态 / 换物单状态 / 品类）")
    @RequestMapping(value = "/distribute", method = RequestMethod.GET)
    public CommonResult<Map<String, Object>> distribute() {
        return CommonResult.success(barterDashboardService.distribute());
    }
}
