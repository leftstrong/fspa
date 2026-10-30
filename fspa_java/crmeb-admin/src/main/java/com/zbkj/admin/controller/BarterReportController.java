package com.zbkj.admin.controller;

import com.zbkj.common.model.barter.EbBarterReport;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.CommonResult;
import com.zbkj.common.response.barter.BarterReportAdminDetailResponse;
import com.zbkj.common.response.barter.BarterReportAdminResponse;
import com.zbkj.common.vo.LoginUserVo;
import com.zbkj.service.service.BarterReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 换物-举报管理 控制器
 */
@RestController
@RequestMapping("api/admin/barter/report")
@Api(tags = "换物-举报管理")
public class BarterReportController {

    @Autowired
    private BarterReportService barterReportService;

    @ApiOperation(value = "列表（原始字段，保留兼容）")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<EbBarterReport>> list(@RequestParam(required = false) Integer status, @ModelAttribute PageParamRequest pageParamRequest) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EbBarterReport> qw = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (status != null) qw.eq("status", status);
        qw.orderByDesc("id");
        com.github.pagehelper.Page<Object> start = com.github.pagehelper.PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        return CommonResult.success(CommonPage.restPage(
                CommonPage.copyPageInfo(start, barterReportService.list(qw))));
    }

    @ApiOperation(value = "富化列表：含物品图与标题、举报人与被举报人昵称头像")
    @RequestMapping(value = "/listRich", method = RequestMethod.GET)
    public CommonResult<CommonPage<BarterReportAdminResponse>> listRich(
            @RequestParam(required = false) Integer status,
            @ModelAttribute PageParamRequest pageParamRequest) {
        return CommonResult.success(barterReportService.adminRichList(status, pageParamRequest));
    }

    @ApiOperation(value = "各状态数量（供 TAB 红点）：all / pending / handled / rejected")
    @RequestMapping(value = "/counts", method = RequestMethod.GET)
    public CommonResult<Map<String, Integer>> counts() {
        return CommonResult.success(barterReportService.adminStatusCounts());
    }

    @ApiOperation(value = "举报明细：物品、关联换物单、双方档案、同一被举报人的其他待处理举报")
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public CommonResult<BarterReportAdminDetailResponse> detail(@RequestParam Integer id) {
        BarterReportAdminDetailResponse d = barterReportService.adminDetail(id);
        if (d == null) return CommonResult.failed("举报不存在");
        return CommonResult.success(d);
    }

    @ApiOperation(value = "处理举报")
    @RequestMapping(value = "/handle", method = RequestMethod.POST)
    public CommonResult<String> handle(@RequestParam Integer id, @RequestParam Integer status,
                                      @RequestParam String result,
                                      @RequestParam(required = false) Integer handleUid) {
        // 处理人优先取登录态，避免前端伪造；老调用方仍可显式传
        Integer adminId = handleUid != null && handleUid > 0 ? handleUid : currentAdminId();
        if (barterReportService.handle(id, status, result, adminId)) return CommonResult.success();
        return CommonResult.failed();
    }

    /**
     * 当前登录管理员 id（JwtAuthenticationTokenFilter 把 LoginUserVo 放进了 SecurityContext）
     */
    private Integer currentAdminId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof LoginUserVo) {
                LoginUserVo vo = (LoginUserVo) auth.getPrincipal();
                return vo.getUser() == null ? null : vo.getUser().getId();
            }
        } catch (Exception ignore) {
        }
        return null;
    }
}
