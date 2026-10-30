package com.zbkj.admin.controller.shop;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.system.SystemStore;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.SystemStoreSearchRequest;
import com.zbkj.common.response.CommonResult;
import com.zbkj.common.response.shop.AdminProductDetailResponse;
import com.zbkj.service.service.ShopService;
import com.zbkj.service.service.SystemStoreService;
import com.zbkj.service.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.Optional;

/**
 * @author Chennong
 * @date 2023-05-12
 * @description 门店接口
 */
@Slf4j
@RestController
@RequestMapping("api/admin/shop")
@Api(tags = "门店接口")
public class ShopController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private UserService userService;

    @Autowired
    private SystemStoreService systemStoreService;

    /**
     * 商品详情
     */
    @ApiOperation(value = "商品详情")
    @GetMapping(value = "/product/detail")
    @ApiImplicitParam(name = "type", value = "normal-正常，video-视频")
    public CommonResult<AdminProductDetailResponse> getDetail(@RequestParam(value = "id") Integer id, @RequestParam(value = "type", defaultValue = "normal") String type) {
        return CommonResult.success(shopService.getDetail(id, type));
    }

    /**
     * 商品详情
     */
    @ApiOperation(value = "查询用户信息")
    @GetMapping(value = "/getUserInfoByPhone")
    public CommonResult<String> getUserInfoByPhone(@Validated @NotBlank(message = "用户手机号不能为空") @RequestParam(value = "phone") String phone) {
        User user = userService.getByPhone(phone);
        Optional.ofNullable(user).orElseThrow(() -> new CrmebException("该用户不存在"));
        return CommonResult.success(user.getNickname(), "成功");
    }

    /**
     * 分页显示门店自提
     * @param request SystemStoreSearchRequest 搜索条件
     * @param pageParamRequest 分页参数
     */
    @PreAuthorize("hasAuthority('admin:system:store:list')")
    @ApiOperation(value = "门店自提分页列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<SystemStore>>  getList(@Validated SystemStoreSearchRequest request, @Validated PageParamRequest pageParamRequest) {
        CommonPage<SystemStore> expressCommonPage = CommonPage.restPage(systemStoreService.getList(request.getKeywords(), request.getStatus(), pageParamRequest));
        return CommonResult.success(expressCommonPage);
    }
}
