package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zbkj.common.model.barter.EbBarterCategory;
import com.zbkj.common.model.barter.EbBarterItem;
import com.zbkj.common.model.barter.EbBarterMessage;
import com.zbkj.common.model.barter.EbBarterOrder;
import com.zbkj.common.model.barter.EbBarterOrderMessage;
import com.zbkj.common.model.barter.EbBarterPointsLog;
import com.zbkj.common.model.barter.EbBarterReport;
import com.zbkj.common.model.user.User;
import com.zbkj.common.response.barter.BarterDashboardResponse;
import com.zbkj.service.dao.BarterOrderMessageDao;
import com.zbkj.service.dao.BarterPointsLogDao;
import com.zbkj.service.service.BarterCategoryService;
import com.zbkj.service.service.BarterDashboardService;
import com.zbkj.service.service.BarterItemService;
import com.zbkj.service.service.BarterMessageService;
import com.zbkj.service.service.BarterOrderService;
import com.zbkj.service.service.BarterReportService;
import com.zbkj.service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 换物-数据驾驶舱 统计实现
 * <p>
 * 统计口径：
 * - 换物用户：有交换次数、有积分余额、或有等级的用户（不含纯商城用户）；
 * - 物品：全表（下架也算，已换出单独计数），不含逻辑删除；
 * - 换物单：全表；进行中 = 状态 2/3（已确认待交付 + 待收货）；纠纷中 = 6；
 * - 留言：eb_barter_order_message（换物单内聊天），与站内通知 eb_barter_message 分开统计；
 * - 积分：正向流水（type=1）合计为发放，负向（type=2）合计为消耗。
 */
@Service
public class BarterDashboardServiceImpl implements BarterDashboardService {

    /** 换物用户判定：有交换次数 / 有积分余额 / 有等级 */
    private static final String USER_SCOPE = "(barter_exchange_count > 0 OR IFNULL(barter_points,0) > 0 OR IFNULL(barter_level_id,0) > 0)";

    /** 按天聚合的 SQL 片段：抽成常量，避免在多处重复拼接出错 */
    private static final String DAY_EXPR = "DATE_FORMAT(create_time,'%m-%d')";
    private static final String DAY_ALIAS = "DATE_FORMAT(create_time,'%m-%d') AS d";
    private static final String COUNT_ALIAS = "COUNT(*) AS c";

    @Autowired
    private UserService userService;
    @Autowired
    private BarterItemService itemService;
    @Autowired
    private BarterOrderService orderService;
    @Autowired
    private BarterMessageService messageService;
    @Autowired
    private BarterReportService reportService;
    @Autowired
    private BarterCategoryService categoryService;
    @Autowired
    private BarterOrderMessageDao orderMessageDao;
    @Autowired
    private BarterPointsLogDao pointsLogDao;

    @Override
    public BarterDashboardResponse summary() {
        BarterDashboardResponse r = new BarterDashboardResponse();
        Date todayStart = startOfToday();

        /* ---------------- 用户 ---------------- */
        r.setUserTotal(userService.getBaseMapper().selectCount(
                new QueryWrapper<User>().apply(USER_SCOPE)));
        r.setUserToday(userService.getBaseMapper().selectCount(
                new QueryWrapper<User>().apply(USER_SCOPE).ge("create_time", todayStart)));

        Map<String, Object> creditAgg = firstRow(userService.getBaseMapper().selectMaps(
                new QueryWrapper<User>().apply(USER_SCOPE)
                        .select("IFNULL(AVG(credit_score),0) AS avg_score",
                                "IFNULL(SUM(CASE WHEN IFNULL(credit_score,0) < 60 THEN 1 ELSE 0 END),0) AS low_cnt")));
        r.setCreditAvg(toDouble(creditAgg.get("avg_score"), 1));
        r.setCreditLowCount(toInt(creditAgg.get("low_cnt")));

        /* ---------------- 物品 ---------------- */
        r.setItemTotal(itemService.count());
        r.setItemToday(itemService.count(new QueryWrapper<EbBarterItem>().ge("create_time", todayStart)));
        r.setItemOnSale(countItemStatus(1));
        r.setItemAuditing(countItemStatus(2));
        r.setItemExchanged(countItemStatus(4));
        r.setItemOff(countItemStatus(6));

        /* ---------------- 换物单 ---------------- */
        r.setOrderTotal(orderService.count());
        r.setOrderToday(orderService.count(new QueryWrapper<EbBarterOrder>().ge("create_time", todayStart)));
        r.setOrderTrading(orderService.count(new QueryWrapper<EbBarterOrder>().in("status", 2, 3)));
        r.setOrderCompleted(orderService.count(new QueryWrapper<EbBarterOrder>().eq("status", 4)));
        r.setOrderDispute(orderService.count(new QueryWrapper<EbBarterOrder>().eq("status", 6)));

        /* ---------------- 互动 ---------------- */
        r.setMessageTotal(orderMessageDao.selectCount(new QueryWrapper<EbBarterOrderMessage>()));
        r.setMessageToday(orderMessageDao.selectCount(
                new QueryWrapper<EbBarterOrderMessage>().ge("create_time", todayStart)));
        r.setNotifyTotal(messageService.count());
        r.setNotifyUnread(messageService.count(new QueryWrapper<EbBarterMessage>().eq("is_read", 0)));

        /* ---------------- 治理 ---------------- */
        r.setReportTotal(reportService.count());
        r.setReportPending(reportService.count(new QueryWrapper<EbBarterReport>().eq("status", 0)));

        /* ---------------- 积分 ---------------- */
        Map<String, Object> pointAgg = firstRow(pointsLogDao.selectMaps(
                new QueryWrapper<EbBarterPointsLog>()
                        .select("IFNULL(SUM(CASE WHEN type = 1 THEN value ELSE 0 END),0) AS issued",
                                "IFNULL(SUM(CASE WHEN type = 2 THEN value ELSE 0 END),0) AS spent")));
        r.setPointsIssued(toInt(pointAgg.get("issued")));
        r.setPointsSpent(toInt(pointAgg.get("spent")));

        Map<String, Object> balAgg = firstRow(userService.getBaseMapper().selectMaps(
                new QueryWrapper<User>().apply(USER_SCOPE)
                        .select("IFNULL(SUM(barter_points),0) AS bal", "IFNULL(SUM(barter_exchange_count),0) AS ex_cnt")));
        r.setPointsBalance(toInt(balAgg.get("bal")));

        /* ---------------- 派生 ---------------- */
        int total = r.getOrderTotal() == null ? 0 : r.getOrderTotal();
        int completed = r.getOrderCompleted() == null ? 0 : r.getOrderCompleted();
        r.setDealRate(total == 0 ? 0D : round1(completed * 100.0 / total));
        int users = r.getUserTotal() == null ? 0 : r.getUserTotal();
        long exchanges = toLong(balAgg.get("ex_cnt"));
        r.setAvgExchange(users == 0 ? 0D : round1(exchanges * 1.0 / users));
        return r;
    }

    @Override
    public Map<String, Object> trend(int days) {
        int n = days <= 0 ? 14 : Math.min(days, 90);
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(n - 1L);
        Date fromTime = java.sql.Timestamp.valueOf(from.atStartOfDay());

        // 先铺满日期轴，缺数据的日期补 0，避免折线断开
        List<String> dates = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 0; i < n; i++) dates.add(from.plusDays(i).format(fmt));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("dates", dates);

        List<Map<String, Object>> userRows = userService.getBaseMapper().selectMaps(
                new QueryWrapper<User>().apply(USER_SCOPE).ge("create_time", fromTime)
                        .select(DAY_ALIAS, COUNT_ALIAS).groupBy(DAY_EXPR));
        out.put("user", fillSeries(dates, groupByDay(userRows)));

        List<Map<String, Object>> itemRows = itemService.getBaseMapper().selectMaps(
                new QueryWrapper<EbBarterItem>().ge("create_time", fromTime)
                        .select(DAY_ALIAS, COUNT_ALIAS).groupBy(DAY_EXPR));
        out.put("item", fillSeries(dates, groupByDay(itemRows)));

        List<Map<String, Object>> orderRows = orderService.getBaseMapper().selectMaps(
                new QueryWrapper<EbBarterOrder>().ge("create_time", fromTime)
                        .select(DAY_ALIAS, COUNT_ALIAS).groupBy(DAY_EXPR));
        out.put("order", fillSeries(dates, groupByDay(orderRows)));

        List<Map<String, Object>> msgRows = orderMessageDao.selectMaps(
                new QueryWrapper<EbBarterOrderMessage>().ge("create_time", fromTime)
                        .select(DAY_ALIAS, COUNT_ALIAS).groupBy(DAY_EXPR));
        out.put("message", fillSeries(dates, groupByDay(msgRows)));
        return out;
    }

    @Override
    public Map<String, Object> distribute() {
        Map<String, Object> out = new LinkedHashMap<>();

        // 物品状态分布
        String[] itemNames = {"在售", "审核中", "已驳回", "已换出", "交换中", "已下架"};
        Map<Integer, Long> itemCnt = new LinkedHashMap<>();
        for (Map<String, Object> row : itemService.getBaseMapper().selectMaps(
                new QueryWrapper<EbBarterItem>().select("status", "COUNT(*) AS c").groupBy("status"))) {
            itemCnt.put(toInt(row.get("status")), toLong(row.get("c")));
        }
        List<Map<String, Object>> itemStatus = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            long v = itemCnt.getOrDefault(i, 0L);
            if (v > 0 || i <= 4) itemStatus.add(pair(itemNames[i - 1], v));
        }
        out.put("itemStatus", itemStatus);

        // 换物单状态分布（0待确认 1已取消 2进行中 3待收货 4已完成 5已拒绝 6纠纷中）
        String[] orderNames = {"待确认", "已取消", "进行中", "待收货", "已完成", "已拒绝", "纠纷中"};
        Map<Integer, Long> orderCnt = new LinkedHashMap<>();
        for (Map<String, Object> row : orderService.getBaseMapper().selectMaps(
                new QueryWrapper<EbBarterOrder>().select("status", "COUNT(*) AS c").groupBy("status"))) {
            orderCnt.put(toInt(row.get("status")), toLong(row.get("c")));
        }
        List<Map<String, Object>> orderStatus = new ArrayList<>();
        for (int i = 0; i <= 6; i++) {
            orderStatus.add(pair(orderNames[i], orderCnt.getOrDefault(i, 0L)));
        }
        out.put("orderStatus", orderStatus);

        // 品类分布（一级品类，按物品件数倒序取前 8）
        Map<Integer, String> catNames = new LinkedHashMap<>();
        for (EbBarterCategory c : categoryService.list()) {
            catNames.put(c.getId(), c.getName());
        }
        List<Map<String, Object>> category = new ArrayList<>();
        for (Map<String, Object> row : itemService.getBaseMapper().selectMaps(
                new QueryWrapper<EbBarterItem>().isNotNull("category_id")
                        .select("category_id", "COUNT(*) AS c").groupBy("category_id")
                        .orderByDesc("c").last("limit 8"))) {
            Integer cid = toInt(row.get("category_id"));
            String name = catNames.containsKey(cid) ? catNames.get(cid) : ("品类" + cid);
            category.add(pair(name, toLong(row.get("c"))));
        }
        out.put("category", category);
        return out;
    }

    /* ==================== 内部工具 ==================== */

    private int countItemStatus(int status) {
        return itemService.count(new QueryWrapper<EbBarterItem>().eq("status", status));
    }

    private Date startOfToday() {
        return java.sql.Timestamp.valueOf(LocalDate.now().atStartOfDay());
    }

    private Map<String, Object> firstRow(List<Map<String, Object>> rows) {
        return (rows == null || rows.isEmpty()) ? new LinkedHashMap<>() : rows.get(0);
    }

    /** 把 [{d,c}] 转成 {日期: 数量}，键名大小写不敏感 */
    private Map<String, Long> groupByDay(List<Map<String, Object>> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (rows == null) return map;
        for (Map<String, Object> row : rows) {
            Object d = pick(row, "d");
            Object c = pick(row, "c");
            if (d == null) continue;
            map.put(String.valueOf(d), toLong(c));
        }
        return map;
    }

    private List<Long> fillSeries(List<String> dates, Map<String, Long> data) {
        List<Long> list = new ArrayList<>(dates.size());
        for (String d : dates) list.add(data.getOrDefault(d, 0L));
        return list;
    }

    private Object pick(Map<String, Object> row, String key) {
        if (row.containsKey(key)) return row.get(key);
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) return e.getValue();
        }
        return null;
    }

    private Map<String, Object> pair(String name, long value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return new BigDecimal(String.valueOf(o)).longValue();
        } catch (Exception e) {
            return 0L;
        }
    }

    private int toInt(Object o) {
        return (int) toLong(o);
    }

    private double toDouble(Object o, int scale) {
        double v = 0D;
        if (o instanceof Number) v = ((Number) o).doubleValue();
        else if (o != null) {
            try {
                v = Double.parseDouble(String.valueOf(o));
            } catch (Exception ignore) {
            }
        }
        return round1(v);
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
