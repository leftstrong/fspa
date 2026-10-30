package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.constants.BarterConstants;
import com.zbkj.common.model.barter.EbBarterItem;
import com.zbkj.common.model.barter.EbBarterMemberLevel;
import com.zbkj.common.model.barter.EbBarterOrder;
import com.zbkj.common.model.barter.EbBarterReport;
import com.zbkj.common.model.system.SystemAdmin;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.barter.BarterReportAdminDetailResponse;
import com.zbkj.common.response.barter.BarterReportAdminResponse;
import com.zbkj.service.dao.BarterReportDao;
import com.zbkj.service.service.BarterConfigService;
import com.zbkj.service.service.BarterCreditLogService;
import com.zbkj.service.service.BarterItemService;
import com.zbkj.service.service.BarterMemberLevelService;
import com.zbkj.service.service.BarterMessageService;
import com.zbkj.service.service.BarterOrderService;
import com.zbkj.service.service.BarterReportService;
import com.zbkj.service.service.SystemAdminService;
import com.zbkj.service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 换物举报 接口实现类
 */
@Service("barterReportService")
public class BarterReportServiceImpl extends ServiceImpl<BarterReportDao, EbBarterReport> implements BarterReportService {

    @Autowired
    private UserService userService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private BarterItemService barterItemService;
    @Autowired
    private BarterOrderService barterOrderService;
    @Autowired
    private BarterMemberLevelService barterMemberLevelService;
    @Autowired
    private SystemAdminService systemAdminService;
    @Autowired
    private BarterMessageService barterMessageService;
    @Autowired
    private BarterConfigService barterConfigService;
    @Autowired
    private BarterCreditLogService barterCreditLogService;

    @Override
    public Boolean createReport(EbBarterReport report) {
        Integer uid = userService.getUserIdException();
        report.setUid(uid);
        report.setStatus(0); // 待处理
        report.setCreateTime(new Date());
        report.setUpdateTime(new Date());
        return transactionTemplate.execute(e -> {
            baseMapper.insert(report);
            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean handle(Integer id, Integer status, String result, Integer handleUid) {
        EbBarterReport report = baseMapper.selectById(id);
        if (report == null) return false;
        if (status == null || (status != 1 && status != 2)) return false;
        // 只有「待处理」首次变更为「已处理」才触发下架/扣分，重复提交不会二次扣分
        boolean wasPending = report.getStatus() == null || report.getStatus() == 0;
        final boolean punish = wasPending && status == 1;

        report.setStatus(status);
        report.setHandleResult(result);
        report.setHandleUid(handleUid);
        report.setHandleTime(new Date());
        report.setUpdateTime(new Date());

        final Integer reportId = report.getId();
        final Integer itemId = report.getItemId();
        final Integer targetUid = report.getTargetUid();
        final Integer reporterUid = report.getUid();
        final int penalty = punish ? Math.max(0, barterConfigService.creditBad()) : 0;

        return transactionTemplate.execute(e -> {
            baseMapper.updateById(report);
            if (punish) {
                punishTarget(reportId, itemId, targetUid, penalty, result);
                if (reporterUid != null && reporterUid > 0) {
                    String tip = result == null || result.trim().isEmpty() ? "" : "处理说明：" + result.trim();
                    barterMessageService.push(reporterUid, BarterConstants.MSG_SYSTEM, reportId,
                            "举报已成立", "您的举报经核查成立，平台已对相关物品/用户作出处理，感谢反馈。" + tip);
                }
            } else if (wasPending && reporterUid != null && reporterUid > 0) {
                // 举报不成立：给举报人一个明确结论，避免反复提交同一举报
                String tip = result == null || result.trim().isEmpty() ? "" : "平台说明：" + result.trim();
                barterMessageService.push(reporterUid, BarterConstants.MSG_SYSTEM, reportId,
                        "举报未成立", "您提交的举报经核查证据不足，未予成立。" + tip);
            }
            return Boolean.TRUE;
        });
    }

    /**
     * 举报成立后的落地动作：下架物品 + 扣被举报人信用分 + 双端站内通知。
     * 信用分值取自后台「规则配置 · 信用分 · 差评扣分」同一项，运营改配置即生效。
     */
    private void punishTarget(Integer reportId, Integer itemId, Integer targetUid, int penalty, String result) {
        String itemTitle = null;
        if (itemId != null && itemId > 0) {
            EbBarterItem item = barterItemService.getById(itemId);
            if (item != null) {
                itemTitle = item.getTitle();
                Integer st = item.getStatus();
                // 仅在售(1)/审核中(2)/已驳回(3) 可下架；交换中(5)、已换出(4) 保持原状，避免破坏进行中的交易
                if (st != null && (st == 1 || st == 2 || st == 3)) {
                    barterItemService.lambdaUpdate()
                            .eq(EbBarterItem::getId, itemId)
                            .set(EbBarterItem::getStatus, BarterConstants.ITEM_STATUS_OFF)
                            .set(EbBarterItem::getTopEndTime, null)
                            .set(EbBarterItem::getUpdateTime, new Date())
                            .update();
                }
            }
        }
        if (targetUid == null || targetUid <= 0) return;
        if (penalty > 0) {
            barterCreditLogService.changeCredit(targetUid, -penalty,
                    "举报成立，信用 -" + penalty + "（举报 #" + reportId + "）", 3, reportId);
        }
        StringBuilder sb = new StringBuilder("您发布的");
        sb.append(itemTitle == null ? "物品" : "「" + itemTitle + "」");
        sb.append("经核查存在违规，平台已下架处理");
        if (penalty > 0) sb.append("，信用分 -").append(penalty);
        if (result != null && !result.trim().isEmpty()) sb.append("。处理说明：").append(result.trim());
        barterMessageService.push(targetUid, BarterConstants.MSG_SYSTEM, itemId == null ? 0 : itemId,
                "举报处理结果", sb.toString());
    }

    /* ==================== 后台：状态计数 ==================== */

    @Override
    public Map<String, Integer> adminStatusCounts() {
        Map<String, Integer> out = new LinkedHashMap<>();
        out.put("all", count());
        out.put("pending", countByStatus(0));
        out.put("handled", countByStatus(1));
        out.put("rejected", countByStatus(2));
        return out;
    }

    private int countByStatus(Integer status) {
        if (status == null) return 0;
        Integer c = count(new QueryWrapper<EbBarterReport>().eq("status", status));
        return c == null ? 0 : c;
    }

    /* ==================== 后台：富化列表 ==================== */

    @Override
    public CommonPage<BarterReportAdminResponse> adminRichList(Integer status, PageParamRequest pageParamRequest) {
        QueryWrapper<EbBarterReport> qw = new QueryWrapper<>();
        if (status != null) qw.eq("status", status);
        qw.orderByAsc("status").orderByDesc("id");

        Page<Object> start = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        List<EbBarterReport> reports = baseMapper.selectList(qw);
        long total = start.getTotal();
        int pageNum = start.getPageNum();
        int pageSize = start.getPageSize();
        int pages = start.getPages();

        List<BarterReportAdminResponse> rows = toRichRows(reports);

        CommonPage<BarterReportAdminResponse> result = new CommonPage<>();
        result.setList(rows);
        result.setTotal(total);
        result.setPage(pageNum);
        result.setLimit(pageSize);
        result.setTotalPage(pages);
        return result;
    }

    /**
     * 批量富化，避免每行都查一次库。
     */
    private List<BarterReportAdminResponse> toRichRows(List<EbBarterReport> reports) {
        List<BarterReportAdminResponse> rows = new ArrayList<>();
        if (reports == null || reports.isEmpty()) return rows;

        Set<Integer> itemIds = new HashSet<>();
        Set<Integer> orderIds = new HashSet<>();
        Set<Integer> uids = new HashSet<>();
        for (EbBarterReport r : reports) {
            if (r.getItemId() != null && r.getItemId() > 0) itemIds.add(r.getItemId());
            if (r.getOrderId() != null && r.getOrderId() > 0) orderIds.add(r.getOrderId());
            if (r.getUid() != null) uids.add(r.getUid());
            if (r.getTargetUid() != null) uids.add(r.getTargetUid());
        }

        Map<Integer, EbBarterItem> itemMap = new HashMap<>();
        if (!itemIds.isEmpty()) {
            for (EbBarterItem it : barterItemService.list(new QueryWrapper<EbBarterItem>().in("id", itemIds))) {
                itemMap.put(it.getId(), it);
            }
        }
        Map<Integer, EbBarterOrder> orderMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            for (EbBarterOrder o : barterOrderService.list(new QueryWrapper<EbBarterOrder>().in("id", orderIds))) {
                orderMap.put(o.getId(), o);
            }
        }
        Map<Integer, User> userMap = new HashMap<>();
        if (!uids.isEmpty()) {
            for (User u : userService.list(new QueryWrapper<User>().in("uid", uids))) {
                userMap.put(u.getUid(), u);
            }
        }

        for (EbBarterReport r : reports) {
            BarterReportAdminResponse v = new BarterReportAdminResponse();
            v.setId(r.getId());
            v.setType(r.getType());
            v.setTypeText(typeText(r.getType()));
            v.setContent(r.getContent());
            v.setStatus(r.getStatus());
            v.setStatusText(statusText(r.getStatus()));
            v.setCreateTime(r.getCreateTime());
            v.setHandleTime(r.getHandleTime());
            v.setHandleResult(r.getHandleResult());
            v.setHandleUid(r.getHandleUid());

            EbBarterItem item = r.getItemId() == null ? null : itemMap.get(r.getItemId());
            if (item != null) {
                v.setItemId(item.getId());
                v.setItemTitle(item.getTitle());
                v.setItemImages(item.getImages());
                v.setItemCover(firstImage(item.getImages()));
                v.setItemStatus(item.getStatus());
                v.setItemStatusText(itemStatusText(item.getStatus()));
            } else if (r.getItemId() != null && r.getItemId() > 0) {
                // 物品已被物理删除，仍要把 id 展示出来，让运营知道举报指向的是什么
                v.setItemId(r.getItemId());
                v.setItemTitle("（物品已删除）");
            }

            EbBarterOrder order = r.getOrderId() == null ? null : orderMap.get(r.getOrderId());
            if (order != null) {
                v.setOrderId(order.getId());
                v.setOrderSn(order.getOrderSn());
                v.setOrderStatusText(orderStatusText(order.getStatus()));
            } else if (r.getOrderId() != null && r.getOrderId() > 0) {
                v.setOrderId(r.getOrderId());
            }

            User reporter = userMap.get(r.getUid());
            v.setUid(r.getUid());
            if (reporter != null) {
                v.setNickname(reporter.getNickname());
                v.setAvatar(reporter.getAvatar());
            }

            User target = userMap.get(r.getTargetUid());
            v.setTargetUid(r.getTargetUid());
            if (target != null) {
                v.setTargetNickname(target.getNickname());
                v.setTargetAvatar(target.getAvatar());
            }

            rows.add(v);
        }
        return rows;
    }

    /* ==================== 后台：明细 ==================== */

    @Override
    public BarterReportAdminDetailResponse adminDetail(Integer id) {
        EbBarterReport r = baseMapper.selectById(id);
        if (r == null) return null;

        BarterReportAdminDetailResponse d = new BarterReportAdminDetailResponse();
        d.setId(r.getId());
        d.setType(r.getType());
        d.setTypeText(typeText(r.getType()));
        d.setContent(r.getContent());
        d.setStatus(r.getStatus());
        d.setStatusText(statusText(r.getStatus()));
        d.setCreateTime(r.getCreateTime());
        d.setHandleTime(r.getHandleTime());
        d.setHandleResult(r.getHandleResult());
        d.setHandleUid(r.getHandleUid());
        d.setHandleUidName(adminName(r.getHandleUid()));

        // 处理这条举报会产生什么数据变更：前端据此在弹框里明确告知运营
        d.setCreditPenalty(Math.max(0, barterConfigService.creditBad()));
        d.setAutoOffshelf(Boolean.TRUE);

        // ---- 物品 ----
        if (r.getItemId() != null && r.getItemId() > 0) {
            EbBarterItem item = barterItemService.getById(r.getItemId());
            if (item != null) {
                BarterReportAdminDetailResponse.Item vi = new BarterReportAdminDetailResponse.Item();
                vi.setId(item.getId());
                vi.setTitle(item.getTitle());
                vi.setDescription(item.getDescription());
                vi.setImages(item.getImages());
                vi.setCover(firstImage(item.getImages()));
                vi.setCity(item.getCity());
                vi.setConditionLevel(item.getConditionLevel());
                vi.setConditionText(conditionText(item.getConditionLevel()));
                vi.setStatus(item.getStatus());
                vi.setStatusText(itemStatusText(item.getStatus()));
                vi.setOwnerUid(item.getUid());
                vi.setViewCount(item.getViewCount());
                vi.setWantCount(item.getWantCount());
                vi.setCreateTime(item.getCreateTime());
                User owner = userService.getById(item.getUid());
                if (owner != null) vi.setOwnerNickname(owner.getNickname());
                Integer itemReports = count(new QueryWrapper<EbBarterReport>().eq("item_id", item.getId()));
                vi.setReportCount(itemReports == null ? 0 : itemReports);
                d.setItem(vi);
            }
        }

        // ---- 换物单 ----
        if (r.getOrderId() != null && r.getOrderId() > 0) {
            EbBarterOrder o = barterOrderService.getById(r.getOrderId());
            if (o != null) {
                BarterReportAdminDetailResponse.Order vo = new BarterReportAdminDetailResponse.Order();
                vo.setId(o.getId());
                vo.setOrderSn(o.getOrderSn());
                vo.setStatus(o.getStatus());
                vo.setStatusText(orderStatusText(o.getStatus()));
                vo.setDeliveryType(o.getDeliveryType());
                vo.setDeliveryTypeText(o.getDeliveryType() != null && o.getDeliveryType() == 1 ? "自提" : "快递");
                vo.setCreateTime(o.getCreateTime());
                d.setOrder(vo);
            }
        }

        // ---- 双方 ----
        d.setReporter(party(r.getUid()));
        d.setTarget(party(r.getTargetUid()));

        // ---- 同一被举报人的其他待处理举报：合并判断，避免漏看 ----
        if (r.getTargetUid() != null) {
            QueryWrapper<EbBarterReport> qw = new QueryWrapper<>();
            qw.eq("target_uid", r.getTargetUid()).eq("status", 0).ne("id", r.getId()).orderByDesc("id").last("LIMIT 5");
            d.setRelatedReports(toRichRows(baseMapper.selectList(qw)));
        }
        return d;
    }

    private BarterReportAdminDetailResponse.Party party(Integer uid) {
        if (uid == null) return null;
        User u = userService.getById(uid);
        if (u == null) return new BarterReportAdminDetailResponse.Party().setUid(uid).setNickname("（用户已不存在）");

        BarterReportAdminDetailResponse.Party p = new BarterReportAdminDetailResponse.Party();
        p.setUid(u.getUid());
        p.setNickname(u.getNickname());
        p.setAvatar(u.getAvatar());
        p.setPhone(maskPhone(u.getPhone()));
        p.setStatus(u.getStatus() != null && u.getStatus() ? 1 : 0);
        p.setCreditScore(u.getCreditScore() == null ? 0 : u.getCreditScore());
        p.setPoints(u.getBarterPoints() == null ? 0 : u.getBarterPoints());
        p.setExchangeCount(u.getBarterExchangeCount() == null ? 0 : u.getBarterExchangeCount());
        p.setLevelId(u.getBarterLevelId());
        p.setCreateTime(u.getCreateTime());
        if (u.getBarterLevelId() != null) {
            EbBarterMemberLevel lv = barterMemberLevelService.getById(u.getBarterLevelId());
            if (lv != null) p.setLevelName(lv.getName());
        }
        Integer rep = count(new QueryWrapper<EbBarterReport>().eq("uid", uid));
        Integer reported = count(new QueryWrapper<EbBarterReport>().eq("target_uid", uid));
        Integer onSale = barterItemService.count(
                new QueryWrapper<EbBarterItem>().eq("uid", uid).eq("status", 1));
        p.setReportTotal(rep == null ? 0 : rep);
        p.setReportedTotal(reported == null ? 0 : reported);
        p.setOnSaleCount(onSale == null ? 0 : onSale);
        return p;
    }

    /* ==================== 文案工具 ==================== */

    private String adminName(Integer adminId) {
        if (adminId == null || adminId <= 0) return null;
        SystemAdmin a = systemAdminService.getById(adminId);
        return a == null ? null : a.getAccount();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String firstImage(String images) {
        if (images == null || images.trim().isEmpty()) return null;
        String[] arr = images.split(",");
        for (String s : arr) {
            if (s != null && !s.trim().isEmpty()) return s.trim();
        }
        return null;
    }

    static String typeText(Integer t) {
        if (t == null) return "其他";
        switch (t) {
            case 1: return "物品违规";
            case 2: return "用户违规";
            case 3: return "欺诈";
            case 4: return "其他";
            default: return "其他";
        }
    }

    static String statusText(Integer s) {
        if (s == null) return "未知";
        switch (s) {
            case 0: return "待处理";
            case 1: return "已处理";
            case 2: return "已驳回";
            default: return "未知";
        }
    }

    static String itemStatusText(Integer s) {
        if (s == null) return "未知";
        switch (s) {
            case 1: return "在售";
            case 2: return "审核中";
            case 3: return "已驳回";
            case 4: return "已换出";
            case 5: return "交换中";
            case 6: return "已下架";
            default: return "未知";
        }
    }

    static String orderStatusText(Integer s) {
        if (s == null) return "未知";
        switch (s) {
            case 0: return "待确认";
            case 1: return "已取消";
            case 2: return "进行中";
            case 3: return "待收货";
            case 4: return "已完成";
            case 5: return "已拒绝";
            case 6: return "纠纷中";
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
}
