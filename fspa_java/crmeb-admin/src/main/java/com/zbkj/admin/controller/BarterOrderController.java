package com.zbkj.admin.controller;

import com.zbkj.common.model.barter.EbBarterOrder;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.BarterOrderSearchRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.CommonResult;
import com.zbkj.common.response.barter.BarterOrderAdminDetailResponse;
import com.zbkj.service.service.BarterOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 换物-换物单管理 控制器
 */
@RestController
@RequestMapping("api/admin/barter/order")
@Api(tags = "换物-换物单管理")
public class BarterOrderController {

    @Autowired
    private BarterOrderService barterOrderService;

    @ApiOperation(value = "分页列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<EbBarterOrder>> getList(@Validated BarterOrderSearchRequest request, @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(barterOrderService.adminList(request, pageParamRequest));
    }

    @ApiOperation(value = "分页列表（富化：含物品标题与双方昵称）")
    @RequestMapping(value = "/listRich", method = RequestMethod.GET)
    public CommonResult<CommonPage<Map<String, Object>>> getRichList(@Validated BarterOrderSearchRequest request,
                                                                    @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(barterOrderService.adminRichList(request, pageParamRequest));
    }

    @ApiOperation(value = "详情（后台：含双方物流、聊天记录、互评）")
    @RequestMapping(value = "/info", method = RequestMethod.GET)
    public CommonResult<EbBarterOrder> info(@RequestParam Integer id) {
        return CommonResult.success(barterOrderService.getById(id));
    }

    @ApiOperation(value = "详情（后台增强版：双方信息 + 物流 + 聊天记录 + 互评）")
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public CommonResult<BarterOrderAdminDetailResponse> detail(@RequestParam Integer id) {
        BarterOrderAdminDetailResponse vo = barterOrderService.adminDetail(id);
        if (vo == null) return CommonResult.failed("换物单不存在");
        return CommonResult.success(vo);
    }

    @ApiOperation(value = "取消/纠纷处理")
    @RequestMapping(value = "/cancel", method = RequestMethod.POST)
    public CommonResult<String> cancel(@RequestParam Integer id, @RequestParam(required = false) String reason) {
        if (barterOrderService.cancel(id, reason)) return CommonResult.success();
        return CommonResult.failed();
    }
}
