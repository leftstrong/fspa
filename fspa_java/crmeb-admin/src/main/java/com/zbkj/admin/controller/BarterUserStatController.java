package com.zbkj.admin.controller;

import com.zbkj.common.model.user.User;
import com.zbkj.common.model.barter.EbBarterCreditLog;
import com.zbkj.common.model.barter.EbBarterItem;
import com.zbkj.common.model.barter.EbBarterMemberLevel;
import com.zbkj.common.model.barter.EbBarterOrder;
import com.zbkj.common.model.barter.EbBarterPointsLog;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.CommonResult;
import com.zbkj.common.response.barter.BarterUserStatResponse;
import com.zbkj.service.service.BarterCreditLogService;
import com.zbkj.service.service.BarterItemService;
import com.zbkj.service.service.BarterMemberLevelService;
import com.zbkj.service.service.BarterOrderService;
import com.zbkj.service.service.BarterPointsLogService;
import com.zbkj.service.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 换物-用户管理 / 信用积分看板 控制器
 * <p>
 * 承担三件事：① 换物用户列表与详情（运营侧的「用户管理」）；② 单个用户的信用/积分汇总与明细；
 * ③ 供其他页面复用的用户下拉选项。
 */
@RestController
@RequestMapping("api/admin/barter/user")
@Api(tags = "换物-用户管理与信用积分")
public class BarterUserStatController {

    @Autowired
    private UserService userService;
    @Autowired
    private BarterMemberLevelService barterMemberLevelService;
    @Autowired
    private BarterPointsLogService pointsLogService;
    @Autowired
    private BarterCreditLogService creditLogService;
    @Autowired
    private BarterOrderService barterOrderService;
    @Autowired
    private BarterItemService barterItemService;

    @ApiOperation(value = "换物用户列表（后台用户管理）")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String city,
                                                             @RequestParam(required = false) Integer levelId,
                                                             @RequestParam(required = false, defaultValue = "1") Integer sort,
                                                             @ModelAttribute PageParamRequest pageParamRequest) {
        Page<Object> startPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        QueryWrapper<User> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            if (kw.matches("\\d+")) {
                // 数字：既可能是 uid，也可能是手机号后几位，两者都试
                qw.and(w -> w.eq("uid", Integer.valueOf(kw)).or().like("phone", kw));
            } else {
                qw.like("nickname", kw);
            }
        }
        if (city != null && !city.trim().isEmpty()) qw.like("barter_city", city.trim());
        if (levelId != null) qw.eq("barter_level_id", levelId);
        // 排序：1 最新注册 / 2 积分 / 3 信用分 / 4 交换次数
        switch (sort == null ? 1 : sort) {
            case 2: qw.orderByDesc("barter_points"); break;
            case 3: qw.orderByDesc("credit_score"); break;
            case 4: qw.orderByDesc("barter_exchange_count"); break;
            default: qw.orderByDesc("uid");
        }
        List<User> users = userService.list(qw);

        Map<Integer, String> levelNames = new HashMap<>();
        List<EbBarterMemberLevel> levels = barterMemberLevelService.list();
        if (levels != null) for (EbBarterMemberLevel l : levels) levelNames.put(l.getId(), l.getName());

        List<Map<String, Object>> rows = new ArrayList<>();
        if (users != null) {
            for (User u : users) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("uid", u.getUid());
                m.put("nickname", u.getNickname());
                m.put("avatar", u.getAvatar());
                m.put("phone", maskPhone(u.getPhone()));
                m.put("city", u.getBarterCity());
                m.put("levelId", u.getBarterLevelId());
                m.put("levelName", levelNames.get(u.getBarterLevelId()));
                m.put("exchangeCount", u.getBarterExchangeCount() == null ? 0 : u.getBarterExchangeCount());
                m.put("points", u.getBarterPoints() == null ? 0 : u.getBarterPoints());
                m.put("pointsFrozen", u.getBarterPointsFrozen() == null ? 0 : u.getBarterPointsFrozen());
                m.put("creditScore", u.getCreditScore() == null ? 0 : u.getCreditScore());
                m.put("status", Boolean.TRUE.equals(u.getStatus()) ? 1 : 0);
                m.put("createTime", u.getCreateTime());
                rows.add(m);
            }
        }
        return CommonResult.success(CommonPage.restPage(CommonPage.copyPageInfo(startPage, rows)));
    }

    @ApiOperation(value = "换物用户详情（档案 + 最近换物单）")
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public CommonResult<Map<String, Object>> detail(@RequestParam Integer uid) {
        User u = userService.getById(uid);
        if (u == null) return CommonResult.failed("用户不存在");
        Map<String, Object> out = new LinkedHashMap<>();
        BarterUserStatResponse stat = new BarterUserStatResponse();
        stat.setUid(u.getUid());
        stat.setNickname(u.getNickname());
        stat.setAvatar(u.getAvatar());
        stat.setExchangeCount(u.getBarterExchangeCount());
        stat.setLevelId(u.getBarterLevelId());
        stat.setCity(u.getBarterCity());
        stat.setPoints(u.getBarterPoints());
        stat.setPointsFrozen(u.getBarterPointsFrozen());
        stat.setCreditScore(u.getCreditScore());
        stat.setFirstAward(u.getBarterFirstAward());
        List<EbBarterMemberLevel> levels = barterMemberLevelService.list();
        if (levels != null && u.getBarterLevelId() != null) {
            levels.stream().filter(l -> l.getId().equals(u.getBarterLevelId())).findFirst()
                    .ifPresent(l -> stat.setLevelName(l.getName()));
        }
        out.put("stat", stat);
        out.put("phone", maskPhone(u.getPhone()));
        out.put("status", Boolean.TRUE.equals(u.getStatus()) ? 1 : 0);
        out.put("createTime", u.getCreateTime());

        // 最近 10 张换物单（任一方命中），供运营快速判断这个人的交易活跃度
        List<EbBarterOrder> orders = barterOrderService.lambdaQuery()
                .and(w -> w.eq(EbBarterOrder::getOwnerUid, uid).or().eq(EbBarterOrder::getApplyUid, uid))
                .orderByDesc(EbBarterOrder::getId)
                .last("limit 10")
                .list();
        List<Map<String, Object>> orderRows = new ArrayList<>();
        String[] statusText = {"待确认", "已取消", "进行中", "待收货", "已完成", "已拒绝", "纠纷中"};
        if (orders != null) {
            for (EbBarterOrder o : orders) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", o.getId());
                m.put("orderSn", o.getOrderSn());
                m.put("status", o.getStatus());
                m.put("statusText", o.getStatus() == null || o.getStatus() < 0 || o.getStatus() > 6
                        ? "未知" : statusText[o.getStatus()]);
                m.put("role", uid.equals(o.getOwnerUid()) ? "物主" : "申请人");
                m.put("peerUid", uid.equals(o.getOwnerUid()) ? o.getApplyUid() : o.getOwnerUid());
                m.put("createTime", o.getCreateTime());
                orderRows.add(m);
            }
        }
        out.put("orders", orderRows);
        return CommonResult.success(out);
    }

    @ApiOperation(value = "启用/禁用换物用户")
    @RequestMapping(value = "/status", method = RequestMethod.POST)
    public CommonResult<String> status(@RequestParam Integer uid, @RequestParam Integer status) {
        User u = userService.getById(uid);
        if (u == null) return CommonResult.failed("用户不存在");
        u.setStatus(status != null && status == 1);
        return userService.updateById(u) ? CommonResult.success() : CommonResult.failed();
    }

    /** 手机号脱敏：保留前 3 后 4 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    @ApiOperation(value = "用户换物汇总（keyword 传 uid 或 昵称）")
    @RequestMapping(value = "/stat", method = RequestMethod.GET)
    public CommonResult<BarterUserStatResponse> stat(@RequestParam String keyword) {
        Integer uid = resolveUid(keyword);
        if (uid == null) return CommonResult.failed("未找到对应用户，请确认 uid 或昵称");
        User u = userService.getById(uid);
        if (u == null) return CommonResult.failed("用户不存在");
        BarterUserStatResponse r = new BarterUserStatResponse();
        r.setUid(u.getUid());
        r.setNickname(u.getNickname());
        r.setAvatar(u.getAvatar());
        r.setExchangeCount(u.getBarterExchangeCount());
        r.setLevelId(u.getBarterLevelId());
        r.setCity(u.getBarterCity());
        r.setPoints(u.getBarterPoints());
        r.setPointsFrozen(u.getBarterPointsFrozen());
        r.setCreditScore(u.getCreditScore());
        r.setFirstAward(u.getBarterFirstAward());
        List<EbBarterMemberLevel> levels = barterMemberLevelService.list();
        if (levels != null && u.getBarterLevelId() != null) {
            levels.stream().filter(l -> l.getId().equals(u.getBarterLevelId())).findFirst()
                    .ifPresent(l -> r.setLevelName(l.getName()));
        }
        return CommonResult.success(r);
    }

    @ApiOperation(value = "用户下拉选项（后台筛选用，按 uid 或昵称模糊搜索）")
    @RequestMapping(value = "/options", method = RequestMethod.GET)
    public CommonResult<List<Map<String, Object>>> options(@RequestParam(required = false) String keyword) {
        List<Map<String, Object>> list = new ArrayList<>();
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        String kw = keyword == null ? "" : keyword.trim();
        if (!kw.isEmpty()) {
            if (kw.matches("\\d+")) {
                qw.eq("uid", Integer.valueOf(kw));
            } else {
                qw.like("nickname", kw);
            }
        }
        qw.orderByDesc("uid").last("limit 20");
        List<User> users = userService.list(qw);
        if (users != null) {
            for (User u : users) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("uid", u.getUid());
                m.put("nickname", u.getNickname());
                m.put("avatar", u.getAvatar());
                m.put("city", u.getBarterCity());
                m.put("creditScore", u.getCreditScore());
                list.add(m);
            }
        }
        return CommonResult.success(list);
    }

    @ApiOperation(value = "积分明细（按 uid 分页）")
    @RequestMapping(value = "/points/log", method = RequestMethod.GET)
    public CommonResult<CommonPage<EbBarterPointsLog>> pointsLog(@RequestParam Integer uid,
                                                                @ModelAttribute PageParamRequest pageParamRequest) {
        return CommonResult.success(pointsLogService.myLog(uid, pageParamRequest));
    }

    @ApiOperation(value = "信用分明细（按 uid 分页）")
    @RequestMapping(value = "/credit/log", method = RequestMethod.GET)
    public CommonResult<CommonPage<EbBarterCreditLog>> creditLog(@RequestParam Integer uid,
                                                                @ModelAttribute PageParamRequest pageParamRequest) {
        return CommonResult.success(creditLogService.myLog(uid, pageParamRequest));
    }

    @ApiOperation(value = "某用户名下的换物物品（分页，可按状态筛）")
    @RequestMapping(value = "/items", method = RequestMethod.GET)
    public CommonResult<CommonPage<Map<String, Object>>> items(@RequestParam Integer uid,
                                                               @RequestParam(required = false) Integer status,
                                                               @ModelAttribute PageParamRequest pageParamRequest) {
        Page<Object> startPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        QueryWrapper<EbBarterItem> qw = new QueryWrapper<>();
        qw.eq("uid", uid);
        if (status != null) qw.eq("status", status);
        qw.orderByDesc("id");
        List<EbBarterItem> items = barterItemService.list(qw);

        List<Map<String, Object>> rows = new ArrayList<>();
        Date now = new Date();
        if (items != null) {
            for (EbBarterItem it : items) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", it.getId());
                m.put("title", it.getTitle());
                m.put("cover", firstImage(it.getImages()));
                m.put("imageCount", imageCount(it.getImages()));
                m.put("status", it.getStatus());
                m.put("statusText", itemStatusText(it.getStatus()));
                m.put("conditionLevel", it.getConditionLevel());
                m.put("conditionText", conditionText(it.getConditionLevel()));
                m.put("city", it.getCity());
                m.put("viewCount", it.getViewCount() == null ? 0 : it.getViewCount());
                m.put("wantCount", it.getWantCount() == null ? 0 : it.getWantCount());
                m.put("likeCount", it.getLikeCount() == null ? 0 : it.getLikeCount());
                m.put("toping", it.getTopEndTime() != null && it.getTopEndTime().after(now));
                m.put("rejectReason", it.getRejectReason());
                m.put("createTime", it.getCreateTime());
                rows.add(m);
            }
        }
        return CommonResult.success(CommonPage.restPage(CommonPage.copyPageInfo(startPage, rows)));
    }

    /** 取第一张图作为封面（库里 images 是逗号分隔的相对路径） */
    private String firstImage(String images) {
        if (images == null || images.trim().isEmpty()) return null;
        for (String s : images.split(",")) {
            if (s != null && !s.trim().isEmpty()) return s.trim();
        }
        return null;
    }

    private int imageCount(String images) {
        if (images == null || images.trim().isEmpty()) return 0;
        int n = 0;
        for (String s : images.split(",")) {
            if (s != null && !s.trim().isEmpty()) n++;
        }
        return n;
    }

    static String itemStatusText(Integer st) {
        if (st == null) return "未知";
        switch (st) {
            case 1: return "在售";
            case 2: return "审核中";
            case 3: return "已驳回";
            case 4: return "已换出";
            case 5: return "交换中";
            case 6: return "已下架";
            default: return "未知";
        }
    }

    static String conditionText(Integer lv) {
        if (lv == null) return "";
        switch (lv) {
            case 1: return "全新";
            case 2: return "九新";
            case 3: return "八新";
            case 4: return "七新及以下";
            case 5: return "其他";
            default: return "";
        }
    }

    /**
     * keyword 解析为 uid：纯数字当 uid；否则按昵称模糊匹配，取 uid 最大（最新）的一条。
     */
    private Integer resolveUid(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return null;
        keyword = keyword.trim();
        if (keyword.matches("\\d+")) return Integer.valueOf(keyword);
        User u = userService.lambdaQuery()
                .like(User::getNickname, keyword)
                .orderByDesc(User::getUid)
                .last("limit 1")
                .one();
        return u != null ? u.getUid() : null;
    }
}
