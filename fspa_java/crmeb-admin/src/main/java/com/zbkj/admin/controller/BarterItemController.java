package com.zbkj.admin.controller;

import com.zbkj.common.model.barter.EbBarterItem;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.BarterItemSearchRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.barter.BarterItemResponse;
import com.zbkj.common.response.CommonResult;
import com.zbkj.service.service.BarterItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 换物-物品管理 控制器（审核/管理）
 */
@RestController
@RequestMapping("api/admin/barter/item")
@Api(tags = "换物-物品管理")
public class BarterItemController {

    @Autowired
    private BarterItemService barterItemService;

    @ApiOperation(value = "分页列表（后台）")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<BarterItemResponse>> getList(@Validated BarterItemSearchRequest request, @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(barterItemService.adminList(request, pageParamRequest));
    }

    @ApiOperation(value = "详情")
    @RequestMapping(value = "/info", method = RequestMethod.GET)
    public CommonResult<BarterItemResponse> info(@RequestParam Integer id) {
        return CommonResult.success(barterItemService.detail(id));
    }

    @ApiOperation(value = "审核通过")
    @RequestMapping(value = "/audit/pass", method = RequestMethod.POST)
    public CommonResult<String> auditPass(@RequestParam Integer id) {
        if (barterItemService.audit(id, 1)) return CommonResult.success();
        return CommonResult.failed();
    }

    @ApiOperation(value = "审核驳回")
    @RequestMapping(value = "/audit/reject", method = RequestMethod.POST)
    public CommonResult<String> auditReject(@RequestParam Integer id,
                                            @RequestParam(required = false) String reason) {
        if (barterItemService.audit(id, 3, reason)) return CommonResult.success();
        return CommonResult.failed();
    }

    @ApiOperation(value = "下架")
    @RequestMapping(value = "/down", method = RequestMethod.POST)
    public CommonResult<String> down(@RequestParam Integer id) {
        if (barterItemService.audit(id, 0)) return CommonResult.success();
        return CommonResult.failed();
    }

    @ApiOperation(value = "调整状态（纠纷结案等平台手动干预：1在售/2审核中/3驳回/4已交换/5交换中/6下架）")
    @RequestMapping(value = "/updateStatus", method = RequestMethod.POST)
    public CommonResult<String> updateStatus(@RequestParam Integer id, @RequestParam Integer status) {
        if (barterItemService.updateStatus(id, status)) return CommonResult.success();
        return CommonResult.failed();
    }
}
