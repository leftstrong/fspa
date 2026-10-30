package com.zbkj.service.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.model.barter.EbBarterCategory;
import com.zbkj.common.model.barter.EbBarterItem;
import com.zbkj.common.model.barter.EbBarterMemberLevel;
import com.zbkj.common.model.barter.EbBarterUserAddress;
import com.zbkj.common.constants.BarterConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.BarterItemSearchRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.barter.BarterItemResponse;
import com.zbkj.service.dao.BarterItemDao;
import com.zbkj.service.service.BarterCategoryService;
import com.zbkj.service.service.BarterConfigService;
import com.zbkj.service.service.BarterItemLikeService;
import com.zbkj.service.service.BarterItemService;
import com.zbkj.service.service.BarterMemberLevelService;
import com.zbkj.service.service.BarterMessageService;
import com.zbkj.service.service.BarterUserAddressService;
import com.zbkj.service.service.BarterPointsLogService;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.service.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 闲置物品 接口实现类
 */
@Service("barterItemService")
public class BarterItemServiceImpl extends ServiceImpl<BarterItemDao, EbBarterItem> implements BarterItemService {

    @Autowired
    private UserService userService;
    @Autowired
    private BarterMemberLevelService memberLevelService;
    @Autowired
    private BarterPointsLogService pointsLogService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private BarterItemLikeService barterItemLikeService;
    @Autowired
    private BarterUserAddressService barterUserAddressService;
    @Autowired
    private BarterMessageService barterMessageService;
    @Autowired
    private BarterCategoryService barterCategoryService;
    @Autowired
    private BarterConfigService barterConfigService;

    /** 置顶单价（积分/小时）：后台可配 barter_top_points_per_hour，默认 1 */
    @Override
    public int topPointsPerHour() {
        return barterConfigService.topPointsPerHour();
    }

    /** 置顶时长（小时）：后台可配 barter_top_duration_hour，默认 24（按天卖，1 小时置顶没有意义） */
    @Override
    public int topDurationHour() {
        return barterConfigService.topDurationHour();
    }

    @Override
    public Boolean publish(EbBarterItem item) {
        Integer uid = userService.getUserIdException();
        item.setUid(uid);
        item.setStatus(2); // 默认审核中
        // 「想换」字段兜底：未选换取方式默认「任意好物」，未选品类落 0（避免 null 影响后续筛选统计）
        if (item.getExpectType() == null) item.setExpectType(3);
        if (item.getExpectCategoryId() == null) item.setExpectCategoryId(0);
        item.setViewCount(0);
        item.setLikeCount(0);
        item.setCreateTime(new Date());
        item.setUpdateTime(new Date());
        return transactionTemplate.execute(e -> {
            baseMapper.insert(item);
            // 站内通知：好物已提交审核（系统消息，可点进物品详情）
            barterMessageService.push(uid, BarterConstants.MSG_SYSTEM, item.getId(),
                    "好物已提交", "您发布的「" + (item.getTitle() != null ? item.getTitle() : "好物") + "」已提交，等待审核");
            return Boolean.TRUE;
        });
    }

    @Override
    public CommonPage<BarterItemResponse> frontList(BarterItemSearchRequest request, PageParamRequest pageParamRequest) {
        Page<Object> startPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        QueryWrapper<EbBarterItem> qw = new QueryWrapper<>();
        qw.eq("status", 1); // 仅展示在售
        // 护栏：排除已锁定在活跃换物订单中的商品（进行中/待确认/待发货/待收货）。
        // 创建订单时商品一般会被翻为交换中(status=5)从而被上面的 status=1 过滤挡掉；
        // 这里再显式兜底一次，避免状态翻转逻辑有疏漏时把“交换中”的商品漏进候选/好物清单。
        // 已完成(4)或已取消/拒绝(5)的订单不在此列——前者商品已易主、后者商品已退回在售。
        qw.notExists("SELECT 1 FROM eb_barter_order o WHERE o.item_id = eb_barter_item.id AND o.status NOT IN (4, 5)");
        if (request != null) {
            if (request.getCategoryId() != null) qw.eq("category_id", request.getCategoryId());
            if (request.getConditionLevel() != null) qw.eq("condition_level", request.getConditionLevel());
            // 空串等价于“不限”：H5 端 undefined 会序列化成 ''，若不过滤会生成 city = '' 查不到任何数据
            if (StrUtil.isNotBlank(request.getCity())) qw.eq("city", request.getCity());
            if (StrUtil.isNotBlank(request.getKeyword())) qw.like("title", request.getKeyword());
            // 「想换」筛选：按期望品类定位（最常用），也可按换取方式筛选
            if (request.getExpectCategoryId() != null) qw.eq("expect_category_id", request.getExpectCategoryId());
            if (request.getExpectType() != null) qw.eq("expect_type", request.getExpectType());
        }
        // 置顶中（未过期）优先排前；过期置顶视同普通商品，不再霸榜
        // 注：orderByDesc 传入含空格的 SQL 表达式时会原样拼接（MyBatis-Plus 对非纯列名不加反引号）
        qw.orderByDesc("CASE WHEN top_end_time IS NOT NULL AND top_end_time > NOW() THEN 1 ELSE 0 END");
        if (request != null && Integer.valueOf(2).equals(request.getSort())) {
            // 最热：按「我想要」人数
            qw.orderByDesc("want_count");
        }
        // 最新：按创建时间倒序（自增 id 与创建顺序在导入数据下可能不一致，以创建时间为准）
        // 必须再按 id 兜底排序：种子数据大量 create_time 相同，缺了唯一 tiebreaker 时 MySQL 的
        // filesort 在不同 LIMIT 偏移下顺序不稳定，会出现「点击加载更多」后同一件物品重复出现
        qw.orderByDesc("create_time");
        qw.orderByDesc("id");
        List<EbBarterItem> items = baseMapper.selectList(qw);
        List<BarterItemResponse> resp = buildResponses(items);
        return CommonPage.restPage(CommonPage.copyPageInfo(startPage, resp));
    }

    @Override
    public CommonPage<BarterItemResponse> myList(PageParamRequest pageParamRequest, Integer status) {
        Integer uid = userService.getUserIdException();
        Page<Object> startPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        QueryWrapper<EbBarterItem> qw = new QueryWrapper<>();
        qw.eq("uid", uid);
        if (status != null) qw.eq("status", status);
        qw.orderByDesc("id");
        List<EbBarterItem> items = baseMapper.selectList(qw);
        return CommonPage.restPage(CommonPage.copyPageInfo(startPage, buildResponses(items)));
    }

    @Override
    public BarterItemResponse detail(Integer id) {
        EbBarterItem item = baseMapper.selectById(id);
        if (item == null) return null;
        List<BarterItemResponse> list = buildResponses(Collections.singletonList(item));
        if (list.isEmpty()) return null;
        BarterItemResponse r = list.get(0);
        // 快递换物前置校验：物主是否设置了默认收货地址
        r.setOwnerHasDefaultAddr(hasDefaultAddress(item.getUid()));
        return r;
    }

    /** 用户是否设置了默认收货地址（is_default=1） */
    private boolean hasDefaultAddress(Integer uid) {
        if (uid == null) return false;
        List<EbBarterUserAddress> list = barterUserAddressService.myList(uid);
        if (list == null || list.isEmpty()) return false;
        return list.stream().anyMatch(a -> a.getIsDefault() != null && a.getIsDefault() == 1);
    }

    @Override
    public Boolean topItem(Integer id, Integer hours) {
        EbBarterItem item = baseMapper.selectById(id);
        if (item == null) return false;
        Integer uid = userService.getUserIdException();
        if (!item.getUid().equals(uid)) return false;
        // 置顶中不重复扣分：前端已隐藏「置顶」按钮，这里再做一次兜底
        if (item.getTopEndTime() != null && item.getTopEndTime().after(new Date())) {
            long leftMin = Math.max(1, (item.getTopEndTime().getTime() - System.currentTimeMillis()) / 60000L);
            String left = leftMin >= 60 ? (leftMin / 60) + " 小时" : leftMin + " 分钟";
            throw new CrmebException("该好物正在置顶中，还剩约 " + left + "，到期后可再次置顶");
        }
        int h = (hours == null || hours <= 0) ? topDurationHour() : hours;
        int cost = topPointsPerHour() * h;
        // 积分不足要说清楚差多少，不能只回一句“积分不足”
        User me = userService.getById(uid);
        int bal = me == null || me.getBarterPoints() == null ? 0 : me.getBarterPoints();
        if (bal < cost) {
            throw new CrmebException("置顶 " + h + " 小时需要 " + cost + " 积分，你当前有 " + bal + " 积分，还差 " + (cost - bal));
        }
        // 扣积分（不足返回 null）
        Integer balance = pointsLogService.changePoints(uid, 2, cost, "置顶商品消耗" + cost + "积分", 1, id);
        if (balance == null) return false;
        Date end = DateUtil.offsetHour(new Date(), h);
        item.setTopEndTime(end);
        item.setTopPoints(cost);
        baseMapper.updateById(item);
        return true;
    }

    @Override
    public List<String> onSaleCities() {
        List<EbBarterItem> items = baseMapper.selectList(new QueryWrapper<EbBarterItem>()
                .eq("status", 1).select("city"));
        return items.stream().map(EbBarterItem::getCity)
                .filter(c -> c != null && !c.trim().isEmpty())
                .distinct().collect(Collectors.toList());
    }

    @Override
    public CommonPage<BarterItemResponse> adminList(BarterItemSearchRequest request, PageParamRequest pageParamRequest) {
        Page<Object> startPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        QueryWrapper<EbBarterItem> qw = new QueryWrapper<>();
        if (request != null) {
            if (request.getStatus() != null) qw.eq("status", request.getStatus());
            if (request.getCategoryId() != null) qw.eq("category_id", request.getCategoryId());
            if (request.getExpectCategoryId() != null) qw.eq("expect_category_id", request.getExpectCategoryId());
            if (request.getExpectType() != null) qw.eq("expect_type", request.getExpectType());
            if (StrUtil.isNotBlank(request.getKeyword())) qw.like("title", request.getKeyword());
        }
        qw.orderByDesc("id");
        List<EbBarterItem> items = baseMapper.selectList(qw);
        return CommonPage.restPage(CommonPage.copyPageInfo(startPage, buildResponses(items)));
    }

    @Override
    public Boolean audit(Integer id, Integer status) {
        return audit(id, status, null);
    }

    @Override
    public Boolean audit(Integer id, Integer status, String reason) {
        EbBarterItem item = baseMapper.selectById(id);
        if (item == null) return false;
        Integer uid = item.getUid();
        String title = item.getTitle() != null ? item.getTitle() : "好物";
        item.setStatus(status); // 1通过 3驳回
        // 驳回时记录原因；非驳回状态清空旧原因（重新审核可追溯）
        if (Integer.valueOf(3).equals(status)) {
            item.setRejectReason(reason);
        } else {
            item.setRejectReason(null);
        }
        item.setUpdateTime(new Date());
        return transactionTemplate.execute(e -> {
            baseMapper.updateById(item);
            // 站内通知：审核结果（系统消息，可点进物品详情）
            if (Integer.valueOf(1).equals(status)) {
                barterMessageService.push(uid, BarterConstants.MSG_SYSTEM, item.getId(),
                        "审核通过", "您发布的「" + title + "」已通过审核并上架");
                // 首发好物奖励：必须审核通过才发，且每人仅一次（用 eb_user.barter_first_award 做幂等标记，
                // 否则同一用户多件商品反复过审会重复送分）
                try {
                    User owner = userService.getById(uid);
                    if (owner != null && (owner.getBarterFirstAward() == null || owner.getBarterFirstAward() != 1)) {
                        // 奖励积分后台可配（barter_points_first_publish）
                        int firstAward = barterConfigService.pointsFirstPublish();
                        Integer bal = pointsLogService.changePoints(uid, 1, firstAward,
                                "首发好物奖励 " + firstAward + " 积分", 7, item.getId());
                        if (bal != null) {
                            // 只更新标记字段：changePoints 内部已单独更新过积分，
                            // 这里若拿发分前的 user 对象整体 updateById 会把刚加的积分覆盖回去
                            userService.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                                    .eq(User::getUid, uid)
                                    .set(User::getBarterFirstAward, 1));
                            barterMessageService.push(uid, BarterConstants.MSG_SYSTEM, item.getId(),
                                    "首发奖励到账", "首发好物已通过审核，奖励 "
                                            + firstAward + " 积分，可用于置顶好物");
                        }
                    }
                } catch (Exception ignore) {
                }
            } else if (Integer.valueOf(3).equals(status)) {
                String reasonTip = (reason != null && !reason.trim().isEmpty()) ? "：" + reason.trim() : "";
                barterMessageService.push(uid, BarterConstants.MSG_SYSTEM, item.getId(),
                        "审核未通过", "您发布的「" + title + "」未通过审核" + reasonTip + "，可修改后重新提交");
            }
            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean updateStatus(Integer id, Integer status) {
        // 仅允许落到已有定义的状态，避免写入脏值（交换中/已交换也可被平台手动调整，用于纠纷结案）
        if (status == null || !(status == BarterConstants.ITEM_STATUS_ON_SALE
                || status == BarterConstants.ITEM_STATUS_AUDITING
                || status == BarterConstants.ITEM_STATUS_REJECTED
                || status == BarterConstants.ITEM_STATUS_EXCHANGED
                || status == BarterConstants.ITEM_STATUS_EXCHANGING
                || status == BarterConstants.ITEM_STATUS_OFF)) {
            return false;
        }
        EbBarterItem item = baseMapper.selectById(id);
        if (item == null) return false;
        item.setStatus(status);
        item.setUpdateTime(new Date());
        return transactionTemplate.execute(e -> {
            baseMapper.updateById(item);
            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean edit(EbBarterItem item) {
        if (item == null || item.getId() == null) return false;
        EbBarterItem exist = baseMapper.selectById(item.getId());
        if (exist == null) return false;
        Integer uid = userService.getUserIdException();
        if (!exist.getUid().equals(uid)) return false; // 仅物品主人可编辑
        // 交换中(5)/已交换(4) 物品不可编辑：处于换物锁定或已终态，避免主人强行改回审核中绕过锁定
        if (exist.getStatus() == BarterConstants.ITEM_STATUS_EXCHANGING
                || exist.getStatus() == BarterConstants.ITEM_STATUS_EXCHANGED) {
            return false;
        }
        exist.setTitle(item.getTitle());
        exist.setCategoryId(item.getCategoryId());
        exist.setConditionLevel(item.getConditionLevel());
        exist.setCity(item.getCity());
        exist.setExpectType(item.getExpectType());
        // 期望品类：允许清空（切回「任意好物」时前端传 0/null），否则编辑保存会丢掉想换品类
        exist.setExpectCategoryId(item.getExpectCategoryId() == null ? 0 : item.getExpectCategoryId());
        if (item.getExpectDesc() != null) exist.setExpectDesc(item.getExpectDesc());
        exist.setDescription(item.getDescription());
        if (item.getImages() != null) exist.setImages(item.getImages());
        exist.setStatus(2); // 保存后重新进入审核
        exist.setUpdateTime(new Date());
        return transactionTemplate.execute(e -> {
            baseMapper.updateById(exist);
            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean reApply(Integer id) {
        EbBarterItem item = baseMapper.selectById(id);
        if (item == null) return false;
        Integer uid = userService.getUserIdException();
        if (!item.getUid().equals(uid)) return false;
        // 交换中(5)/已交换(4) 不可重新提交，同样受锁定保护
        if (item.getStatus() == BarterConstants.ITEM_STATUS_EXCHANGING
                || item.getStatus() == BarterConstants.ITEM_STATUS_EXCHANGED) {
            return false;
        }
        item.setStatus(2); // 重新进入审核
        item.setUpdateTime(new Date());
        return transactionTemplate.execute(e -> {
            baseMapper.updateById(item);
            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean relist(Integer id) {
        EbBarterItem item = baseMapper.selectById(id);
        if (item == null) return false;
        Integer uid = userService.getUserIdException();
        if (!item.getUid().equals(uid)) return false;
        if (item.getStatus() == null || item.getStatus() != BarterConstants.ITEM_STATUS_EXCHANGED) return false;
        // 只有「通过换物收到的」才允许一键重新上架；自己换出去的旧物品不在我名下，走不到这里
        if (item.getReceiveFromOrderId() == null) {
            throw new CrmebException("这件好物不是通过换物收到的，请重新发布");
        }
        item.setStatus(BarterConstants.ITEM_STATUS_ON_SALE);
        item.setUpdateTime(new Date());
        return transactionTemplate.execute(e -> {
            baseMapper.updateById(item);
            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean offshelf(Integer id) {        EbBarterItem item = baseMapper.selectById(id);
        if (item == null) return false;
        Integer uid = userService.getUserIdException();
        if (!item.getUid().equals(uid)) return false;
        if (item.getStatus() != 1) return false; // 仅在售态可下架（交换中/审核中等不可下架）
        item.setStatus(6); // 已下架
        item.setTopEndTime(null); // 下架即失效置顶
        item.setUpdateTime(new Date());
        return transactionTemplate.execute(e -> {
            baseMapper.updateById(item);
            return Boolean.TRUE;
        });
    }

    /** 批量拼装响应：补充发布者昵称/头像/信用/等级 + 是否置顶中 */
    @Override
    public List<BarterItemResponse> buildResponses(List<EbBarterItem> items) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        Set<Integer> uids = items.stream().map(EbBarterItem::getUid).collect(Collectors.toSet());
        Map<Integer, User> userMap = userService.listByIds(uids).stream().collect(Collectors.toMap(User::getUid, u -> u));
        List<EbBarterMemberLevel> levels = memberLevelService.tree();
        Map<Integer, EbBarterMemberLevel> levelMap = levels.stream().collect(Collectors.toMap(EbBarterMemberLevel::getId, l -> l));
        // 当前登录用户（未登录为 null）：用于回显「是否想要」
        Integer currentUid = userService.getUserId();
        Set<Integer> wantItemIds = Collections.emptySet();
        if (currentUid != null && !items.isEmpty()) {
            wantItemIds = new HashSet<>(barterItemLikeService.wantedItemIds(currentUid,
                    items.stream().map(EbBarterItem::getId).collect(Collectors.toList())));
        }
        Date now = new Date();
        // 期望换取品类名称映射：仅当本批数据确实有待解析的品类时才查表（品类表仅十来行）
        Map<Integer, String> catNameMap = Collections.emptyMap();
        boolean needCatName = items.stream()
                .anyMatch(it -> it.getExpectCategoryId() != null && it.getExpectCategoryId() > 0);
        if (needCatName) {
            List<EbBarterCategory> cats = barterCategoryService.frontList();
            if (cats != null && !cats.isEmpty()) {
                catNameMap = cats.stream()
                        .collect(Collectors.toMap(EbBarterCategory::getId, EbBarterCategory::getName, (a, b) -> a));
            }
        }
        List<BarterItemResponse> result = new ArrayList<>();
        for (EbBarterItem it : items) {
            BarterItemResponse r = new BarterItemResponse();
            BeanUtils.copyProperties(it, r);
            User u = userMap.get(it.getUid());
            if (u != null) {
                r.setOwnerNickname(u.getNickname());
                r.setOwnerAvatar(u.getAvatar());
                r.setOwnerExchangeCount(u.getBarterExchangeCount());
                r.setOwnerCreditScore(u.getCreditScore());
                EbBarterMemberLevel lv = levelMap.get(u.getBarterLevelId());
                if (lv != null) {
                    r.setOwnerLevelName(lv.getName());
                    r.setOwnerLevelIcon(lv.getIcon());
                }
            }
            r.setToping(it.getTopEndTime() != null && it.getTopEndTime().after(now));
            r.setIsWant(wantItemIds.contains(it.getId()));
            if (it.getExpectCategoryId() != null && it.getExpectCategoryId() > 0) {
                r.setExpectCategoryName(catNameMap.get(it.getExpectCategoryId()));
            }
            result.add(r);
        }
        return result;
    }
}
