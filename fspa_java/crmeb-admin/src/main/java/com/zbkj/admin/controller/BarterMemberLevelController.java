package com.zbkj.admin.controller;

import com.zbkj.common.model.barter.EbBarterMemberLevel;
import com.zbkj.common.response.CommonResult;
import com.zbkj.service.service.BarterMemberLevelService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 换物-会员等级配置 控制器
 */
@RestController
@RequestMapping("api/admin/barter/level")
@Api(tags = "换物-会员等级配置")
public class BarterMemberLevelController {

    @Autowired
    private BarterMemberLevelService barterMemberLevelService;

    @ApiOperation(value = "等级列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public CommonResult<List<EbBarterMemberLevel>> list() {
        return CommonResult.success(barterMemberLevelService.tree());
    }

    @ApiOperation(value = "新增等级")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public CommonResult<String> save(@RequestBody EbBarterMemberLevel level) {
        if (barterMemberLevelService.save(level)) return CommonResult.success();
        return CommonResult.failed();
    }

    @ApiOperation(value = "编辑等级")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public CommonResult<String> update(@RequestBody EbBarterMemberLevel level) {
        if (barterMemberLevelService.updateById(level)) return CommonResult.success();
        return CommonResult.failed();
    }

    @ApiOperation(value = "删除等级")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public CommonResult<String> delete(@RequestParam Integer id) {
        // 默认等级不可删除、等级不存在等由 Service 抛 CrmebException，全局异常处理器统一返回
        if (barterMemberLevelService.deleteLevel(id)) return CommonResult.success();
        return CommonResult.failed();
    }
}
