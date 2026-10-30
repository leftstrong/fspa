package com.zbkj.admin.controller;

import com.zbkj.common.model.article.EbAppointmentsEntity;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.AppointmentDetailResponse;
import com.zbkj.common.response.AppointmentSearchRequest;
import com.zbkj.common.response.CommonResult;
import com.zbkj.service.service.EbAppointmentsService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 预约表 控制器
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2022 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: 大粽子
 * +----------------------------------------------------------------------
 * ｜ @date Thu May 18 08:22:55 CST 2023
 * +----------------------------------------------------------------------
 * ｜ @date stivepeim@outlook.com
 * +----------------------------------------------------------------------
 */
@RestController
@RequestMapping("api/admin/appointments")
public class AppointmentsController {
    @Autowired
    private EbAppointmentsService ebAppointmentsService;


    @ApiOperation(value = "分页列表") //配合swagger使用
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<AppointmentDetailResponse>> getList(@Validated AppointmentSearchRequest request, @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(ebAppointmentsService.adminList(request, pageParamRequest));
    }

    /**
     * 详情数据
     */
    @RequestMapping(value = "/info", method = RequestMethod.GET)
//    @PreAuthorize("hasAuthority('admin:appointments:info')")
    public CommonResult<EbAppointmentsEntity> info(@RequestParam(value = "id") Integer id) {
        EbAppointmentsEntity ebAppointments = ebAppointmentsService.getById(id);

        return CommonResult.success(ebAppointments);
    }

    /**
     * 新增数据
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST)
//    @PreAuthorize("hasAuthority('admin:appointments:save')")
    public CommonResult<String> save(@RequestBody EbAppointmentsEntity ebAppointments) {
        if (ebAppointmentsService.save(ebAppointments)) {
            return CommonResult.success();
        }
        return CommonResult.failed();
    }

    /**
     * 修改数据
     */
    @RequestMapping("/update')")
    @PreAuthorize("hasAuthority('admin:appointments:update')")
    public CommonResult<String> update(@RequestBody EbAppointmentsEntity ebAppointments) {
        if (ebAppointmentsService.updateById(ebAppointments)) {
            return CommonResult.success();
        }
        return CommonResult.failed();
    }

    /**
     * 删除:根据id集合
     */
    @RequestMapping(value = "/delete", method = RequestMethod.GET)
//    @PreAuthorize("hasAuthority('admin:appointments:delete')")
    public CommonResult<String> delete(@RequestParam(value = "id") Integer id) {
        if (ebAppointmentsService.removeById(id)) {
            return CommonResult.success();
        }
        return CommonResult.failed();
    }

}
