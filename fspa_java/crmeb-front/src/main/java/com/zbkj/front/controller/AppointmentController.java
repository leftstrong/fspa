package com.zbkj.front.controller;

import com.zbkj.common.model.article.Article;
import com.zbkj.common.model.article.EbAppointmentsEntity;
import com.zbkj.common.model.category.Category;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.AppointmentDetailResponse;
import com.zbkj.common.response.ArtisanDetailResponse;
import com.zbkj.common.response.CommonResult;
import com.zbkj.service.service.CategoryService;
import com.zbkj.service.service.EbAppointmentsService;
import com.zbkj.service.service.EbArtisanService;
import com.zbkj.service.service.StoreProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;


/**
 * 文章
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
@Slf4j
@RestController("AppointmentController")
@RequestMapping("api/front/appointment")
@Api(tags = "预约模块")
public class AppointmentController {

    @Autowired
    private EbArtisanService artisanService;
    @Autowired
    private EbAppointmentsService appointmentService;
    @Autowired
    private StoreProductService storeProductService;
    @Autowired
    private CategoryService categoryService;

    @ApiOperation(value = "我的预约列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<AppointmentDetailResponse>> getList(@ModelAttribute PageParamRequest pageRequest) {
        return CommonResult.success(CommonPage.restPage(appointmentService.myList(pageRequest)));
    }


    @ApiOperation(value = "预约明细")
    @RequestMapping(value = "/info", method = RequestMethod.GET)
    @ApiImplicitParam(name = "id", value = "预约id")
    public CommonResult<AppointmentDetailResponse> info(@RequestParam(value = "id") Integer id) {
        return CommonResult.success(appointmentService.getInfo(id));
    }


    @ApiOperation(value = "提交预约")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public CommonResult<String> create(@RequestBody @Validated EbAppointmentsEntity appointmentsEntity) {
        if (Objects.nonNull(appointmentsEntity.getArtisanId()) && appointmentsEntity.getArtisanId().equals(0)) {
            appointmentsEntity.setArtisanId(null);
        }
        if (appointmentService.create(appointmentsEntity)) {
            return CommonResult.success();
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation(value = "删除预约")
    @RequestMapping(value = "/del", method = RequestMethod.POST)
    public CommonResult<Boolean> delete(@RequestParam Integer id) {
        if (appointmentService.delete(id)) {
            return CommonResult.success();
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation(value = "取消预约")
    @RequestMapping(value = "/cancel", method = RequestMethod.POST)
    public CommonResult<Boolean> cancel(@RequestParam Integer id) {
        if (appointmentService.cancel(id)) {
            return CommonResult.success();
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation(value = "手艺人列表")
    @RequestMapping(value = "/artisan/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<ArtisanDetailResponse>> getArtisanList(@RequestParam String appointmentTime) {
        return CommonResult.success(CommonPage.restPage(artisanService.listByAppointmentTime(appointmentTime)));
    }

    @ApiOperation(value = "手艺人信息")
    @RequestMapping(value = "/artisan/getCraftsmanInfoById", method = RequestMethod.GET)
    public CommonResult<ArtisanDetailResponse> getCraftsmanInfoById(@RequestParam Integer craftsmanId) {
        return CommonResult.success(artisanService.getCraftsmanInfoById(craftsmanId));
    }

    @ApiOperation(value = "项目列表")
    @RequestMapping(value = "/product/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<StoreProduct>> getProductList() {
        Set<Integer> categoryIds = categoryService.getShopProductCategoryIds();
        return CommonResult.success(CommonPage.restPage(storeProductService.listByCateIds(categoryIds)));
    }

    @ApiOperation(value = "可预约时间列表")
    @RequestMapping(value = "/getWorkDayTimeList", method = RequestMethod.GET)
    public CommonResult<List> getWorkDayTimeList() {
        return CommonResult.success(appointmentService.getWorkDayTimeList());
    }

    @ApiOperation(value = "项目列表")
    @RequestMapping(value = "/product/getProductListByIds", method = RequestMethod.GET)
    public CommonResult<CommonPage<StoreProduct>> getProductListByIds(EbAppointmentsEntity appointmentsEntity) {
        return CommonResult.success(CommonPage.restPage(storeProductService.getProductListByIds(appointmentsEntity)));
    }
}



