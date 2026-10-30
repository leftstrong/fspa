package com.zbkj.admin.controller;

import com.zbkj.common.model.barter.EbBarterCategory;
import com.zbkj.common.response.CommonResult;
import com.zbkj.service.service.BarterCategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 换物-品类管理 控制器
 */
@RestController
@RequestMapping("api/admin/barter/category")
@Api(tags = "换物-品类管理")
public class BarterCategoryController {

    @Autowired
    private BarterCategoryService barterCategoryService;

    @ApiOperation(value = "品类列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public CommonResult<List<EbBarterCategory>> list() {
        return CommonResult.success(barterCategoryService.list());
    }

    @ApiOperation(value = "新增品类")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public CommonResult<String> save(@RequestBody EbBarterCategory category) {
        if (barterCategoryService.save(category)) return CommonResult.success();
        return CommonResult.failed();
    }

    @ApiOperation(value = "编辑品类")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public CommonResult<String> update(@RequestBody EbBarterCategory category) {
        if (barterCategoryService.updateById(category)) return CommonResult.success();
        return CommonResult.failed();
    }

    @ApiOperation(value = "删除品类")
    @RequestMapping(value = "/delete", method = RequestMethod.GET)
    public CommonResult<String> delete(@RequestParam Integer id) {
        if (barterCategoryService.removeById(id)) return CommonResult.success();
        return CommonResult.failed();
    }
}
