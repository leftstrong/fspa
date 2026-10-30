package com.zbkj.admin.controller.shop;

import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.CartNumRequest;
import com.zbkj.common.request.CartRequest;
import com.zbkj.common.request.CartResetRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.shop.AdminCartDeleteRequest;
import com.zbkj.common.request.shop.AdminCartNumRequest;
import com.zbkj.common.response.CartInfoResponse;
import com.zbkj.common.response.CommonResult;
import com.zbkj.common.utils.SecurityUtil;
import com.zbkj.service.service.ShopService;
import com.zbkj.service.service.StoreCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Chennong
 * @date 2023-05-12
 * @description 门店购物车接口
 */
@Slf4j
@RestController
@RequestMapping("api/admin/shop/cart")
@Api(tags = "商品 -- 购物车") //配合swagger使用
public class ShopCartController {

    @Autowired
    private StoreCartService storeCartService;
    @Autowired
    private ShopService shopService;

    /**
     * 分页显示购物车表
     */
    @ApiOperation(value = "分页列表") //配合swagger使用
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "isValid", value = "类型，true-有效商品，false-无效商品", required = true),
            @ApiImplicitParam(name = "page", value = "页码", required = true),
            @ApiImplicitParam(name = "limit", value = "每页数量", required = true)
    })
    public CommonResult<CommonPage<CartInfoResponse>> getList(@RequestParam Boolean isValid, @Validated PageParamRequest pageParamRequest) {
        CommonPage<CartInfoResponse> restPage = CommonPage.restPage(storeCartService.getAdminCarList(pageParamRequest, isValid));
        return CommonResult.success(restPage);
    }

    /**
     * 加入商品到购物车
     *
     * @param storeCartRequest 新增参数
     */
    @ApiOperation(value = "新增")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public CommonResult<HashMap<String, String>> save(@RequestBody @Validated CartRequest storeCartRequest) {
        String cartId = storeCartService.adminSaveCate(storeCartRequest);
        if (StringUtils.isNotBlank(cartId)) {
            HashMap<String, String> result = new HashMap<>();
            result.put("cartId", cartId);
            return CommonResult.success(result);
        } else {
            return CommonResult.failed();
        }
    }

    /**
     * 清空购物车
     * @return
     */
    @ApiOperation(value = "清空购物车")
    @DeleteMapping(value = "/flush")
    public CommonResult<String> flush() {
        User adminOrUser = shopService.getAdminOrUser(null);
        if (storeCartService.flushCart(adminOrUser.getUid())) {
            return CommonResult.success();
        } else {
            return CommonResult.failed();
        }
    }

    /**
     * 商品移出购物车
     * @return
     */
    @ApiOperation(value = "商品移出购物车")
    @DeleteMapping(value = "/delete")
    public CommonResult<String> delete(@RequestBody @Validated AdminCartDeleteRequest request) {
        if (storeCartService.deleteCartByIds(Collections.singletonList(request.getId()))) {
            return CommonResult.success();
        } else {
            return CommonResult.failed();
        }
    }

    /**
     * 删除购物车表
     *
     * @param ids 购物车ids
     */
    @ApiOperation(value = "删除")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public CommonResult<String> delete(@RequestParam(value = "ids") List<Long> ids) {
        if (storeCartService.deleteCartByIds(ids)) {
            return CommonResult.success();
        } else {
            return CommonResult.failed();
        }
    }

    /**
     * 修改购物车商品数量
     * @param request
     * @return
     */
    @ApiOperation(value = "修改购物车商品数量")
    @PostMapping(value = "/num")
    public CommonResult<String> update(@RequestBody AdminCartNumRequest request) {
        if (storeCartService.adminUpdateCartNum(request)) {
            return CommonResult.success();
        } else {
            return CommonResult.failed();
        }
    }

//    /**
//     * 获取购物车数量
//     */
//    @ApiOperation(value = "获取购物车数量")
//    @RequestMapping(value = "/count", method = RequestMethod.GET)
//    public CommonResult<Map<String, Integer>> count(@Validated CartNumRequest request) {
//        return CommonResult.success(storeCartService.getUserCount(request));
//    }
//
//    /**
//     * 购物车重选提交
//     *
//     * @param resetRequest 重选参数
//     * @return 结果
//     */
//    @ApiOperation(value = "购物车重选提交")
//    @RequestMapping(value = "/resetcart", method = RequestMethod.POST)
//    public CommonResult<Object> resetCart(@RequestBody @Validated CartResetRequest resetRequest) {
//        return CommonResult.success(storeCartService.resetCart(resetRequest));
//    }
}



