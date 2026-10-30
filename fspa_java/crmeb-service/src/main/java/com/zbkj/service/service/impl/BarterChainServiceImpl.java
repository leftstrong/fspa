package com.zbkj.service.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zbkj.common.constants.BarterConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.barter.EbBarterCategory;
import com.zbkj.common.model.barter.EbBarterItem;
import com.zbkj.common.model.barter.EbBarterOrder;
import com.zbkj.common.model.barter.EbBarterUserAddress;
import com.zbkj.common.model.user.User;
import com.zbkj.common.request.BarterChainCreateRequest;
import com.zbkj.common.request.BarterChainSearchRequest;
import com.zbkj.common.response.barter.BarterChainCreateResponse;
import com.zbkj.common.response.barter.BarterChainSearchResponse;
import com.zbkj.service.dao.BarterItemDao;
import com.zbkj.service.dao.BarterOrderDao;
import com.zbkj.service.service.BarterCategoryService;
import com.zbkj.service.service.BarterChainService;
import com.zbkj.service.service.BarterConfigService;
import com.zbkj.service.service.BarterMessageService;
import com.zbkj.service.service.BarterPointsLogService;
import com.zbkj.service.service.BarterUserAddressService;
import com.zbkj.service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 智能换物（多方交换环）实现
 *
 * 一、模型
 * 交换环 = 每个参与者「给出自己的一件，得到自己期望的一件」的闭环。
 * 物品流向 A → X1 → … → B → A：
 *   - A 给 X1 的主人，A 收到 B；
 *   - X1 给 X2 的主人，X1 的主人收到 A；
 *   - … B 给 A 的主人，B 的主人收到倒数第二件。
 * 因此用户指定的 B 一定在环上（否则拿不到想要的东西），A 最终收到 B。
 *
 * 边判定：物品 U 能交给 V 的主人 ⟺ V 的持有者「愿意接受 U」（accepts(V, U)）。
 * 于是找环 = 在「接受关系」图上，从 A 出发找一条长度 1~3 的路径到达 B，并自动闭合 B→A。
 *
 * 二、跳数
 * 1 跳 = 2 人（A、B 主人）直接互换；2 跳 = 3 人；3 跳 = 4 人。跳数 = 人数 − 1 = 生成的订单张数 − 1。
 *
 * 三、计费
 * 检索免费（用户可以反复试），只有确认成环并被服务端重新校验通过后才扣积分：
 * 1/2/3 跳 → 10/20/50 积分，按「实际跳数」扣而不是按用户选的上限扣（选了 3 跳却 2 跳就成环，只扣 20）。
 *
 * 四、不做价值对齐
 * 匹配只看「品类 / 期望描述」是否互相接受，不估算物品价值，不做差价补偿。
 */
@Service("barterChainService")
public class BarterChainServiceImpl implements BarterChainService {

    /** 参与检索的在售物品上限：换物场景下单城市在售量远小于此，一次拉全量在内存里跑图，避免 N 次 DB 往返 */
    private static final int POOL_LIMIT = 1500;
    /** 每个节点最多展开的候选数：控制 DFS 分支数，保证长链路检索也在毫秒级 */
    private static final int CAND_LIMIT = 16;
    /**
     * 4 跳及以上的分支数：深度每加一层，分支数是指数放大（16^4 ≈ 6.5 万），
     * 长链路收窄到 8，用「广度换速度」，仍然优先同城/置顶/成色好的物件。
     */
    private static final int CAND_LIMIT_DEEP = 8;
    /**
     * 4 跳及以上搜索的节点扩展预算（次 candidates() 扫描）。
     * 每次扫描要遍历整个候选池并做接受度判定，不设上限时最坏情况要扫上亿次，
     * 会让接口卡到超时；这样封顶后，最深档（4-6 人）也在几百毫秒内给出结果或「没找到」。
     */
    private static final int EXPAND_BUDGET_DEEP = 1200;
    /** 一次检索最多返回的候选环数量 */
    private static final int MAX_PATHS = 5;

    /** 「随便换」类描述：出现这些词且描述不长时，视为对物品无限制 */
    private static final String[] ANY_WORDS = {"任意", "随便", "都行", "不限", "无要求", "看对方", "都可以", "无所谓"};
    private static final int ANY_WORDS_MAX_LEN = 12;

    private static final String[] ORDER_STATUS_TEXT = {"待确认", "已取消", "进行中", "待收货", "已完成", "已拒绝", "纠纷中"};

    @Autowired
    private BarterItemDao itemDao;
    @Autowired
    private BarterOrderDao orderDao;
    @Autowired
    private BarterCategoryService categoryService;
    @Autowired
    private BarterUserAddressService addressService;
    @Autowired
    private BarterPointsLogService pointsLogService;
    @Autowired
    private BarterMessageService messageService;
    @Autowired
    private UserService userService;
    @Autowired
    private BarterConfigService barterConfigService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    /* ==================== P1：链路检索（免费） ==================== */

    @Override
    public BarterChainSearchResponse search(BarterChainSearchRequest request) {
        Integer uid = userService.getUserIdException();
        if (request == null || request.getMyItemId() == null || request.getTargetItemId() == null) {
            throw new CrmebException("请先选择要拿出的物品和想要的物品");
        }
        EbBarterItem a = itemDao.selectById(request.getMyItemId());
        if (a == null || !uid.equals(a.getUid())) {
            throw new CrmebException("只能用自己的在售物品发起智能换物");
        }
        if (a.getStatus() == null || a.getStatus() != BarterConstants.ITEM_STATUS_ON_SALE) {
            throw new CrmebException("你的这件物品当前不是在售状态，不能拿出去换");
        }
        EbBarterItem b = itemDao.selectById(request.getTargetItemId());
        if (b == null) throw new CrmebException("想要的物品不存在");
        if (b.getStatus() == null || b.getStatus() != BarterConstants.ITEM_STATUS_ON_SALE) {
            throw new CrmebException("《" + b.getTitle() + "》已不在售");
        }
        if (uid.equals(b.getUid())) throw new CrmebException("不能和自己换物");

        int hopLimit = barterConfigService.chainMaxHops();
        int maxHops = request.getMaxHops() == null ? hopLimit : request.getMaxHops();
        maxHops = Math.max(1, Math.min(maxHops, hopLimit));

        // 候选池：所有在售、且不是我发布的（我不能在同一个环里出现两次）
        QueryWrapper<EbBarterItem> qw = new QueryWrapper<>();
        qw.eq("status", BarterConstants.ITEM_STATUS_ON_SALE);
        qw.ne("uid", uid);
        qw.ne("id", b.getId());
        qw.orderByDesc("CASE WHEN top_end_time IS NOT NULL AND top_end_time > NOW() THEN 1 ELSE 0 END");
        qw.orderByDesc("id");
        qw.last("LIMIT " + POOL_LIMIT);
        List<EbBarterItem> pool = itemDao.selectList(qw);

        // 由短到长搜索：一旦某跳数找到解就停，只给用户最快的方案（更长的环只在换不到时才有意义）
        List<List<EbBarterItem>> rings = new ArrayList<>();
        for (int hops = 1; hops <= maxHops; hops++) {
            collect(hops, a, b, pool, rings);
            if (!rings.isEmpty()) break;
        }

        User me = userService.getById(uid);
        BarterChainSearchResponse resp = new BarterChainSearchResponse();
        resp.setMyPoints(me == null || me.getBarterPoints() == null ? 0 : me.getBarterPoints());
        resp.setPoolSize(pool.size() + 2);
        resp.setTargetAcceptsMe(accepts(b, a));
        resp.setPaths(new ArrayList<>());
        resp.setOk(!rings.isEmpty());

        if (rings.isEmpty()) {
            resp.setTip(buildNoResultTip(a, b, maxHops, resp.getTargetAcceptsMe()));
            return resp;
        }

        // 统一装配环上的用户与品类信息（一次批量查询，避免 N+1）
        Set<Integer> uids = new LinkedHashSet<>();
        Set<Integer> catIds = new LinkedHashSet<>();
        for (List<EbBarterItem> ring : rings) {
            for (EbBarterItem it : ring) {
                uids.add(it.getUid());
                if (it.getCategoryId() != null && it.getCategoryId() > 0) catIds.add(it.getCategoryId());
            }
        }
        Map<Integer, User> userMap = loadUsers(uids);
        Map<Integer, String> catMap = loadCategoryNames(catIds);
        Map<Integer, Boolean> addrMap = new HashMap<>();

        for (List<EbBarterItem> ring : rings) {
            resp.getPaths().add(buildPath(ring, uid, userMap, catMap, addrMap));
        }
        int minHops = resp.getPaths().get(0).getHops();
        int minPoints = resp.getPaths().get(0).getPoints();
        String t = minHops == 1
                ? "你们可以直接互换，不需要中间人"
                : "最短 " + minHops + " 跳、经 " + (minHops - 1) + " 位中间人即可成交";
        // 只在成交那一刻收费，先明确这一点，避免用户以为搜一下就要扣分
        t += minPoints > 0
                ? "；第一棒成交时收 " + minPoints + " 分路径规划费"
                : "；2 人直接互换免费，成交也不扣分";
        if (minPoints > 0 && resp.getMyPoints() < minPoints) {
            t += "（你当前 " + resp.getMyPoints() + " 分，余额不足的部分成交时免收）";
        }
        resp.setTip(t);
        return resp;
    }

    /** 按固定跳数收集候选环（跳数 = 环上边数 = 人数 - 1） */
    private void collect(int hops, EbBarterItem a, EbBarterItem b, List<EbBarterItem> pool, List<List<EbBarterItem>> out) {
        if (hops == 1) {
            // 两人互换：B 的主人愿意接受 A
            if (accepts(b, a)) out.add(Arrays.asList(a, b));
            return;
        }
        List<EbBarterItem> path = new ArrayList<>();
        path.add(a);
        Set<Integer> usedUid = new HashSet<>();
        usedUid.add(a.getUid());
        usedUid.add(b.getUid()); // B 的主人是终点，不能在环里再出现一次
        Set<Integer> usedItem = new HashSet<>();
        usedItem.add(a.getId());
        usedItem.add(b.getId());
        // 深链（4 跳起）加节点预算，避免无解时把 16^4 条分支全跑一遍
        int[] budget = {hops >= 4 ? EXPAND_BUDGET_DEEP : Integer.MAX_VALUE / 2};
        extend(path, usedUid, usedItem, hops, b, pool, out, budget);
    }

    /**
     * 深度优先扩展路径。
     * @param targetHops 目标边数
     * @param usedUid    已入环的用户（一人只能在环里出现一次）
     * @param usedItem   已入环的物品
     */
    private void extend(List<EbBarterItem> path, Set<Integer> usedUid, Set<Integer> usedItem,
                        int targetHops, EbBarterItem b, List<EbBarterItem> pool,
                        List<List<EbBarterItem>> out, int[] budget) {
        if (out.size() >= MAX_PATHS) return;
        int needMore = targetHops - (path.size() - 1); // 还差几条边到 B（含到 B 的那一条）
        if (needMore <= 0) return;
        EbBarterItem cur = path.get(path.size() - 1);

        if (needMore == 1) {
            if (accepts(b, cur)) {
                List<EbBarterItem> ring = new ArrayList<>(path);
                ring.add(b);
                out.add(ring);
            }
            return;
        }
        // needMore >= 2：还要再放中间节点；当 needMore == 2 时即将放入的是倒数第二个，必须被 B 主人接受
        if (budget[0] <= 0) return;
        budget[0]--;
        int limit = targetHops >= 4 ? CAND_LIMIT_DEEP : CAND_LIMIT;
        for (EbBarterItem y : candidates(cur, pool, usedUid, usedItem, needMore == 2 ? b : null, limit)) {
            path.add(y);
            usedUid.add(y.getUid());
            usedItem.add(y.getId());
            extend(path, usedUid, usedItem, targetHops, b, pool, out, budget);
            path.remove(path.size() - 1);
            usedUid.remove(y.getUid());
            usedItem.remove(y.getId());
            if (out.size() >= MAX_PATHS || budget[0] <= 0) return;
        }
    }

    /**
     * 当前物品的下一个交接对象：必须「它的主人愿意接受 cur」。
     * @param mustAcceptBy 当该节点将成为倒数第二个时，必须同时被它接受（提前剪枝，避免无解分支）
     */
    private List<EbBarterItem> candidates(EbBarterItem cur, List<EbBarterItem> pool,
                                         Set<Integer> usedUid, Set<Integer> usedItem,
                                         EbBarterItem mustAcceptBy, int limit) {
        final String curCity = cur.getCity() == null ? "" : cur.getCity();
        List<EbBarterItem> list = new ArrayList<>();
        for (EbBarterItem y : pool) {
            if (y.getId() == null || usedItem.contains(y.getId())) continue;
            if (y.getUid() == null || usedUid.contains(y.getUid())) continue;
            if (y.getStatus() == null || y.getStatus() != BarterConstants.ITEM_STATUS_ON_SALE) continue;
            if (!accepts(y, cur)) continue;                       // y 的主人得愿意收 cur
            if (mustAcceptBy != null && !accepts(mustAcceptBy, y)) continue; // 倒数第二个：必须能被 B 主人接受
            list.add(y);
        }
        // 同城优先（少一次寄件）→ 置顶 → 成色好 → 新发布
        list.sort((x, y) -> {
            int cx = sameCity(curCity, x.getCity()) ? 0 : 1;
            int cy = sameCity(curCity, y.getCity()) ? 0 : 1;
            if (cx != cy) return cx - cy;
            int tx = isToping(x) ? 0 : 1, ty = isToping(y) ? 0 : 1;
            if (tx != ty) return tx - ty;
            int lx = x.getConditionLevel() == null ? 9 : x.getConditionLevel();
            int ly = y.getConditionLevel() == null ? 9 : y.getConditionLevel();
            if (lx != ly) return lx - ly;
            return (y.getId() == null ? 0 : y.getId()) - (x.getId() == null ? 0 : x.getId());
        });
        return list.size() > limit ? new ArrayList<>(list.subList(0, limit)) : list;
    }

    private boolean isToping(EbBarterItem it) {
        return it.getTopEndTime() != null && it.getTopEndTime().after(new Date());
    }

    /**
     * holder 是否愿意接受 item（边 item → holder 的判定）。
     * 判定口径刻意不依赖 expectType 的枚举歧义（前端 0 基、库注释 1 基并存）：
     *   1) 填了期望品类 → 按品类匹配（含父品类）；
     *   2) 填了具体描述 → 按关键词匹配标题/描述；
     *   3) 其余（任意好物 / 信用优先 / 未填）→ 一律接受。
     * 这正是「不做价值对齐」的落地：只看品类与文字意愿，不估价。
     */
    private boolean accepts(EbBarterItem holder, EbBarterItem item) {
        if (holder == null || item == null) return false;
        Integer wantCat = holder.getExpectCategoryId();
        if (wantCat != null && wantCat > 0) {
            Integer cid = item.getCategoryId();
            Integer pid = item.getCategoryPid();
            return wantCat.equals(cid) || wantCat.equals(pid);
        }
        String desc = holder.getExpectDesc();
        if (StrUtil.isNotBlank(desc) && !isAnyIntent(desc)) {
            String hay = (item.getTitle() == null ? "" : item.getTitle()) + " "
                    + (item.getDescription() == null ? "" : item.getDescription());
            return keywordHit(desc, hay);
        }
        return true;
    }

    /** 「随便换 / 任意好物 / 都行」这类不做限制的短描述 */
    private boolean isAnyIntent(String desc) {
        String d = desc.trim();
        if (d.length() > ANY_WORDS_MAX_LEN) return false;
        for (String w : ANY_WORDS) {
            if (d.contains(w)) return true;
        }
        return false;
    }

    /** 从期望描述里抽词，命中物品标题/描述即认为匹配 */
    private boolean keywordHit(String desc, String hay) {
        if (StrUtil.isBlank(hay)) return false;
        String[] tokens = desc.split("[^\\u4e00-\\u9fa5A-Za-z0-9]+");
        boolean hasToken = false;
        for (String t : tokens) {
            if (t.length() < 2) continue;
            hasToken = true;
            if (hay.contains(t)) return true;
        }
        return !hasToken && hay.contains(desc.trim());
    }

    private String buildNoResultTip(EbBarterItem a, EbBarterItem b, int maxHops, boolean targetAcceptsMe) {
        // 用「人」说事，比「跳」直观：用户只关心这条链要拉几个人进来
        String head = "最多 " + (barterConfigService.chainMaxHops() + 1) + " 人";
        if (targetAcceptsMe) {
            return "《" + b.getTitle() + "》的主人愿意收下《" + a.getTitle() + "》，但没找到能接力的环："
                    + "把跨步放宽到 " + head + "再试，或换一件更容易出手的起点物品。";
        }
        return "《" + b.getTitle() + "》的主人想要的是别的品类，不会直接收下《" + a.getTitle() + "》，"
                + "需要有人从中转一手；在 " + (maxHops + 1) + " 人以内没找到合适的接力人。"
                + "可以放宽到 " + head + "，或让起点物品更贴近对方想要的品类。";
    }

    private BarterChainSearchResponse.Path buildPath(List<EbBarterItem> ring, Integer myUid,
                                                    Map<Integer, User> userMap,
                                                    Map<Integer, String> catMap,
                                                    Map<Integer, Boolean> addrCache) {
        int size = ring.size();
        BarterChainSearchResponse.Path path = new BarterChainSearchResponse.Path();
        path.setHops(size - 1);
        path.setPoints(barterConfigService.pathFee(size - 1));
        path.setSummary(size == 2
                ? "你和" + nick(userMap.get(ring.get(1).getUid()), myUid) + "直接互换，一步到位"
                : "经由 " + (size - 2) + " 位中间人，" + size + " 人接力成交");

        List<BarterChainSearchResponse.Node> nodes = new ArrayList<>();
        Set<String> cities = new LinkedHashSet<>();
        List<String> warnings = new ArrayList<>();
        Set<Integer> noAddr = new LinkedHashSet<>();

        for (int k = 0; k < size; k++) {
            EbBarterItem it = ring.get(k);
            EbBarterItem prev = ring.get((k - 1 + size) % size);
            EbBarterItem next = ring.get((k + 1) % size);
            User owner = userMap.get(it.getUid());

            BarterChainSearchResponse.Node n = new BarterChainSearchResponse.Node();
            n.setItemId(it.getId());
            n.setTitle(it.getTitle());
            n.setCover(firstImage(it.getImages()));
            n.setCategoryId(it.getCategoryId());
            n.setCategoryName(catMap.get(it.getCategoryId()));
            n.setOwnerUid(it.getUid());
            n.setOwnerNickname(owner == null ? "良物用户" : owner.getNickname());
            n.setOwnerAvatar(owner == null ? "" : owner.getAvatar());
            n.setCity(it.getCity());
            n.setConditionText(conditionText(it.getConditionLevel()));
            n.setDeliveryType(it.getDeliveryType());
            n.setDeliveryText(it.getDeliveryType() != null && it.getDeliveryType() == 1 ? "自提" : "快递");
            n.setExpectText(expectText(it));
            n.setRole(k == 0 ? "me" : (k == size - 1 ? "target" : "peer"));
            // 该参与者：给出自己的物品，从上一位手里拿到 prev 这件（中文里不加空格，昵称紧贴“从/处”）
            String fromNick = nick(userMap.get(prev.getUid()), myUid);
            String giver = k == 0 ? "你" : nick(owner, myUid);
            n.setHandoff(giver + "给出《" + it.getTitle() + "》，从" + fromNick + "处拿到《" + prev.getTitle() + "》");
            nodes.add(n);

            if (StrUtil.isNotBlank(it.getCity())) cities.add(normCity(it.getCity()));
            if (!hasDefaultAddress(it.getUid(), addrCache)) noAddr.add(it.getUid());
            // 这一个人的物品要交给下一位，如果下一位只支持自提又异地，实际交接会卡住
            if (next.getDeliveryType() != null && next.getDeliveryType() == 1
                    && !sameCity(it.getCity(), next.getCity())) {
                warnings.add("《" + next.getTitle() + "》只支持自提，但和《" + it.getTitle() + "》不在同一城市，交接需要另外协商");
            }
        }
        path.setNodes(nodes);
        path.setCrossCity(cities.size() > 1);
        if (cities.size() > 1) {
            warnings.add(0, "横跨 " + cities.size() + " 个城市（" + String.join("、", cities) + "），每段都要寄一次快递");
        }
        if (!noAddr.isEmpty()) {
            warnings.add(noAddr.size() + " 位参与者还没有默认收货地址，成环后请提醒对方补齐，否则发货会卡住");
        }
        path.setWarnings(warnings);
        return path;
    }

    /* ==================== 只给方案 + 只走第一棒 ==================== */

    /**
     * 按方案发起「第一棒」：只生成 1 张 1v1 换物单（我的物品 ↔ 下一棒的物品）。
     *
     * <p>为什么不再一次生成 N 张单：
     * <ul>
     *   <li>后续几棒的人随时可能下架/改条件/删物品，N 张单一开就会把所有人的物品提前锁死；</li>
     *   <li>环形流向是单向接力，一张单同时要求 A 侧和 B 侧各寄一次，同一件货会被要求寄两遍。</li>
     * </ul>
     * 所以后端只对第一棒担保：锁物品、等对方确认、可发货可收货；后面的路让用户自己走，
     * 任一人退出只影响他自己那一棒，不会连环崩。</p>
     *
     * <p>收费：检索与出方案免费，<b>第一棒成交时</b>才按方案人数扣路径规划费
     * （2 人免费 / 3 人 10 分 / 4-6 人 50 分）。建立换物单时一分不扣，被拒/取消也不产生退款。</p>
     */
    @Override
    public BarterChainCreateResponse createChain(BarterChainCreateRequest request) {
        Integer uid = userService.getUserIdException();
        if (request == null || request.getMyItemId() == null || request.getTargetItemId() == null
                || CollUtil.isEmpty(request.getRingItemIds())) {
            throw new CrmebException("缺少换物方案信息，请重新检索");
        }
        List<Integer> ids = request.getRingItemIds();
        if (new HashSet<>(ids).size() != ids.size()) throw new CrmebException("换物方案里出现了重复物品");
        if (ids.size() < 2) throw new CrmebException("换物方案至少要两个人");
        if (!ids.get(0).equals(request.getMyItemId())) throw new CrmebException("方案的起点必须是你自己的物品");
        if (!ids.get(ids.size() - 1).equals(request.getTargetItemId())) {
            throw new CrmebException("方案的终点必须是你想要的物品");
        }
        int hops = ids.size() - 1;
        if (hops > barterConfigService.chainMaxHops()) throw new CrmebException("换物方案人数超出上限");
        int maxHops = request.getMaxHops() == null ? hops : request.getMaxHops();
        if (hops > maxHops) throw new CrmebException("这条方案需要 " + hops + " 跳，超出你选择的上限");

        // 只重新校验第一棒的两件货；后面的成员不锁、不校验（他们随时可能变卦）
        EbBarterItem mine = itemDao.selectById(ids.get(0));
        EbBarterItem next = itemDao.selectById(ids.get(1));
        if (mine == null) throw new CrmebException("你的这件好物已不存在");
        if (mine.getStatus() == null || mine.getStatus() != BarterConstants.ITEM_STATUS_ON_SALE) {
            throw new CrmebException("《" + mine.getTitle() + "》已不在售，请重新选择起点");
        }
        if (!uid.equals(mine.getUid())) throw new CrmebException("方案的起点必须是你自己的在售物品");
        if (next == null) throw new CrmebException("下一棒的好物已不存在，请重新检索");
        if (next.getStatus() == null || next.getStatus() != BarterConstants.ITEM_STATUS_ON_SALE) {
            throw new CrmebException("《" + next.getTitle() + "》已不在售，这条方案刚刚失效，请重新检索");
        }
        if (next.getUid().equals(uid)) throw new CrmebException("第一棒的对方不能是你自己");
        if (!accepts(next, mine)) {
            throw new CrmebException("《" + next.getTitle() + "》的主人已不再接受《" + mine.getTitle()
                    + "》，方案失效，请重新检索");
        }
        // 双方任一件还挂在别的未完成单里，就别再开新单（防止一件货被两张单同时要求寄出）
        Integer busy = orderDao.selectCount(new QueryWrapper<EbBarterOrder>()
                .in("item_id", mine.getId(), next.getId())
                .in("status", 0, 2, 3, 6));
        if (busy != null && busy > 0) throw new CrmebException("有一件好物还在别的换物单里，等那单结束再来");

        // 按人分档：2 人免费、3 人 10 分、4-6 人 50 分（跳数 = 人数 − 1），费用可在后台规则配置调整
        int fee = barterConfigService.pathFee(hops);
        User me = userService.getById(uid);
        int balance = me == null || me.getBarterPoints() == null ? 0 : me.getBarterPoints();

        boolean isPickup = next.getDeliveryType() != null && next.getDeliveryType() == 1;
        String myAddress = null;
        Integer addrId = request.getAddressId();
        if (!isPickup) {
            // 环式换物页没有地址选择器：没传就用账号的默认收货地址兜底，
            // 避免「明明设了默认地址还被要求选择地址」这种假报错
            if (addrId == null) {
                addrId = defaultAddressId(uid);
                if (addrId == null) {
                    throw new CrmebException("请先在「我的-收货地址」添加并设一个默认地址，再发起快递换物");
                }
            }
            myAddress = addressText(addrId, uid);
            if (myAddress == null) throw new CrmebException("收货地址不存在，请重新选择");
        }

        List<String> warnings = new ArrayList<>();
        if (!hasDefaultAddress(next.getUid(), new HashMap<>())) {
            warnings.add("对方还没有默认收货地址，发货前需要 TA 补齐");
        }
        if (fee > 0 && balance < fee) {
            warnings.add("路径费 " + fee + " 分已冻结 " + Math.min(balance, fee) + " 分（你当前有 " + balance
                    + " 积分），不足部分不再收取；未成交会原路退回");
        }
        if (ids.size() > 2) {
            warnings.add("后面还有 " + (ids.size() - 2) + " 棒不锁定物品，等这一棒完成后再用「继续换下一件」接着走");
        }

        String chainNo = "C" + System.currentTimeMillis() + String.format("%03d", new Random().nextInt(1000));
        Date now = new Date();
        EbBarterOrder o = new EbBarterOrder();
        o.setOrderSn(buildOrderSn(0, uid));
        o.setItemId(next.getId());
        o.setOwnerUid(next.getUid());
        o.setApplyUid(uid);
        o.setOfferItemIds(String.valueOf(mine.getId()));
        o.setStatus(0); // 待确认：等下一棒的主人确认
        o.setDeliveryType(next.getDeliveryType() == null ? 2 : next.getDeliveryType());
        o.setOwnerDeliverStatus(0);
        o.setApplyDeliverStatus(0);
        o.setOwnerConfirmRecv(0);
        o.setApplyConfirmRecv(0);
        o.setApplyMessage(request.getApplyMessage());
        o.setApplicantAddressId(addrId);
        o.setApplyAddress(myAddress);
        o.setChainNo(chainNo);
        o.setChainSeq(1);
        o.setChainSize(ids.size());
        o.setPathFeePoints(fee);
        o.setPathFeeCharged(0);
        o.setPathFeePaid(0);
        o.setPathFeeFrozen(0);
        o.setCreateTime(now);
        o.setUpdateTime(now);

        // 发起即冻结路径费：不冻结的话，用户可以在成交前把积分拿去置顶，成交时就没分可扣了
        final int[] frozenAmt = new int[]{0};
        transactionTemplate.execute(e -> {
            orderDao.insert(o);
            if (fee > 0) {
                Integer after = pointsLogService.freezePoints(uid, fee,
                        "智能换物冻结路径费 " + fee + " 分（成交转实扣，未成交退回）", o.getId());
                if (after != null) {
                    frozenAmt[0] = Math.min(balance, fee);
                    o.setPathFeeFrozen(frozenAmt[0]);
                    orderDao.updateById(o);
                }
            }
            // 只锁第一棒的两件货：交换中(5)，避免被普通换物重复选中
            for (EbBarterItem it : new EbBarterItem[]{mine, next}) {
                it.setStatus(BarterConstants.ITEM_STATUS_EXCHANGING);
                it.setUpdateTime(now);
                itemDao.updateById(it);
            }
            return Boolean.TRUE;
        });

        // 事务外推送
        Map<Integer, User> userMap = loadUsers(new HashSet<>(Arrays.asList(uid, next.getUid())));
        try {
            String myName = nick(userMap.get(uid), next.getUid());
            messageService.push(next.getUid(), BarterConstants.MSG_ORDER_PENDING, o.getId(),
                    "有人想和你走换物接力",
                    myName + "想用 TA 的《" + mine.getTitle() + "》换你的《" + next.getTitle()
                            + "》（第 1/" + ids.size() + " 棒），确认后即可开始交接");
        } catch (Exception ignore) {
            // 推送失败不影响下单
        }
        try {
            String body = ids.size() == 2
                    ? "你发起与" + nick(userMap.get(next.getUid()), uid) + "的直接互换：给出《" + mine.getTitle()
                    + "》，收到《" + next.getTitle() + "》。等对方确认后开始交接"
                    : "方案第一棒已发起：你给出《" + mine.getTitle() + "》，收到《" + next.getTitle()
                    + "》。等对方确认；走完这一棒后再用「继续换下一件」发起第二棒";
            messageService.push(uid, BarterConstants.MSG_ORDER_CREATED, o.getId(),
                    ids.size() == 2 ? "互换已发起" : "接力第一棒已发起", body);
        } catch (Exception ignore) {
            // 同上
        }

        BarterChainCreateResponse resp = new BarterChainCreateResponse();
        resp.setChainNo(chainNo);
        resp.setChainSize(ids.size());
        resp.setOrderCount(1);
        resp.setHops(hops);
        resp.setPoints(fee);
        resp.setCharged(0);
        resp.setBalance(balance);
        resp.setOrderIds(Collections.singletonList(o.getId()));
        resp.setWarnings(warnings);
        return resp;
    }

    @Override
    public BarterChainCreateResponse.Detail detail(String chainNo) {
        Integer uid = userService.getUserIdException();
        if (StrUtil.isBlank(chainNo)) throw new CrmebException("环号不能为空");
        List<EbBarterOrder> orders = orderDao.selectList(new QueryWrapper<EbBarterOrder>()
                .eq("chain_no", chainNo).orderByAsc("chain_seq"));
        if (orders.isEmpty()) throw new CrmebException("换物环不存在");
        boolean involved = orders.stream().anyMatch(o ->
                uid.equals(o.getOwnerUid()) || uid.equals(o.getApplyUid()));
        if (!involved) throw new CrmebException("你不在这个换物环里");

        Set<Integer> itemIds = new LinkedHashSet<>();
        Set<Integer> uids = new LinkedHashSet<>();
        for (EbBarterOrder o : orders) {
            itemIds.add(o.getItemId());
            if (StrUtil.isNotBlank(o.getOfferItemIds())) {
                for (String s : o.getOfferItemIds().split(",")) {
                    if (StrUtil.isNotBlank(s)) itemIds.add(Integer.valueOf(s.trim()));
                }
            }
            uids.add(o.getOwnerUid());
            uids.add(o.getApplyUid());
        }
        Map<Integer, EbBarterItem> itemMap = new HashMap<>();
        if (!itemIds.isEmpty()) {
            for (EbBarterItem it : itemDao.selectBatchIds(itemIds)) itemMap.put(it.getId(), it);
        }
        Map<Integer, User> userMap = loadUsers(uids);

        BarterChainCreateResponse.Detail d = new BarterChainCreateResponse.Detail();
        d.setChainNo(chainNo);
        d.setChainSize(orders.get(0).getChainSize() == null ? orders.size() : orders.get(0).getChainSize());
        d.setHops(d.getChainSize() - 1);
        List<BarterChainCreateResponse.Edge> edges = new ArrayList<>();
        boolean dissolved = false, completed = true, running = false;
        for (EbBarterOrder o : orders) {
            BarterChainCreateResponse.Edge e = new BarterChainCreateResponse.Edge();
            e.setOrderId(o.getId());
            e.setOrderSn(o.getOrderSn());
            e.setSeq(o.getChainSeq());
            EbBarterItem give = null;
            if (StrUtil.isNotBlank(o.getOfferItemIds())) {
                give = itemMap.get(Integer.valueOf(o.getOfferItemIds().split(",")[0].trim()));
            }
            EbBarterItem take = itemMap.get(o.getItemId());
            e.setFromItemTitle(give == null ? "-" : give.getTitle());
            e.setToItemTitle(take == null ? "-" : take.getTitle());
            e.setFromNickname(nick(userMap.get(o.getApplyUid()), uid));
            e.setToNickname(nick(userMap.get(o.getOwnerUid()), uid));
            e.setStatus(o.getStatus());
            e.setStatusText(statusText(o.getStatus()));
            e.setMyRole(uid.equals(o.getOwnerUid()) ? "owner" : (uid.equals(o.getApplyUid()) ? "apply" : "none"));
            e.setCreateTime(o.getCreateTime());
            edges.add(e);

            Integer st = o.getStatus();
            if (st != null && (st == 1 || st == 5)) dissolved = true;
            if (st == null || st != 4) completed = false;
            if (st != null && (st == 2 || st == 3 || st == 6)) running = true;
        }
        d.setEdges(edges);
        d.setDissolved(dissolved);
        d.setStatusText(dissolved ? "已解散" : (completed ? "已完成" : (running ? "进行中" : "待确认")));
        return d;
    }

    /* ==================== 内部工具 ==================== */

    private Map<Integer, User> loadUsers(Set<Integer> uids) {
        Map<Integer, User> map = new HashMap<>();
        if (uids == null || uids.isEmpty()) return map;
        List<User> users = userService.listByIds(new ArrayList<>(uids));
        if (users != null) for (User u : users) map.put(u.getUid(), u);
        return map;
    }

    private Map<Integer, String> loadCategoryNames(Set<Integer> catIds) {
        Map<Integer, String> map = new HashMap<>();
        if (catIds == null || catIds.isEmpty()) return map;
        List<EbBarterCategory> list = categoryService.listByIds(new ArrayList<>(catIds));
        if (list != null) for (EbBarterCategory c : list) map.put(c.getId(), c.getName());
        return map;
    }

    /** 昵称：「你」优先，其次昵称，兜底「良物用户」 */
    private String nick(User u, Integer myUid) {
        if (u != null && myUid != null && myUid.equals(u.getUid())) return "你";
        if (u == null || StrUtil.isBlank(u.getNickname())) return "良物用户";
        return u.getNickname();
    }

    private String firstImage(String images) {
        if (StrUtil.isBlank(images)) return "";
        String[] arr = images.split(",");
        return arr.length == 0 ? "" : arr[0].trim();
    }

    /** 城市名归一：去空格、去尾部「市」，避开「上海 / 上海市」这种同一城市被算成两城 */
    private String normCity(String city) {
        if (StrUtil.isBlank(city)) return "";
        String s = city.trim().replaceAll("\\s+", "");
        if (s.length() > 2 && s.endsWith("市")) s = s.substring(0, s.length() - 1);
        return s;
    }

    /** 同城判断：归一后相等，或一方是另一方的上级写法（海口 / 海口中） */
    private boolean sameCity(String a, String b) {
        String x = normCity(a);
        String y = normCity(b);
        if (x.isEmpty() || y.isEmpty()) return false;
        return x.equals(y) || x.startsWith(y) || y.startsWith(x);
    }

    private String conditionText(Integer lv) {
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

    /** 物品主人的换取意愿文案（解释「为什么会连上这条边」） */
    private String expectText(EbBarterItem it) {
        Integer catId = it.getExpectCategoryId();
        if (catId != null && catId > 0) {
            return "想要同品类好物";
        }
        String desc = it.getExpectDesc();
        if (StrUtil.isBlank(desc) || isAnyIntent(desc)) return "什么都可以换";
        return desc.length() > 20 ? desc.substring(0, 20) + "…" : desc;
    }

    private String statusText(Integer status) {
        if (status == null || status < 0 || status >= ORDER_STATUS_TEXT.length) return "未知";
        return ORDER_STATUS_TEXT[status];
    }

    private String buildOrderSn(int seq, Integer uid) {
        return "CB" + System.currentTimeMillis() + String.format("%02d", seq + 1) + uid;
    }

    /** 用户是否设了默认收货地址（带缓存，环上最多 4 人） */
    private boolean hasDefaultAddress(Integer uid, Map<Integer, Boolean> cache) {
        if (uid == null) return false;
        Boolean c = cache.get(uid);
        if (c != null) return c;
        boolean ok = false;
        try {
            List<EbBarterUserAddress> list = addressService.myList(uid);
            ok = list != null && list.stream().anyMatch(x -> x.getIsDefault() != null && x.getIsDefault() == 1);
        } catch (Exception ignore) {
        }
        cache.put(uid, ok);
        return ok;
    }

    /**
     * 取账号默认收货地址 id。
     * myList 已按 is_default desc 排序，所以有默认地址时它一定在第一个；
     * 没有默认地址时不猜（不拿第一个地址顶替），避免把货寄到错的地方。
     */
    private Integer defaultAddressId(Integer uid) {
        if (uid == null) return null;
        try {
            List<EbBarterUserAddress> list = addressService.myList(uid);
            if (list == null || list.isEmpty()) return null;
            EbBarterUserAddress first = list.get(0);
            return (first.getIsDefault() != null && first.getIsDefault() == 1) ? first.getId() : null;
        } catch (Exception ignore) {
            return null;
        }
    }

    /** 取某地址的快照文本，并校验归属 */
    private String addressText(Integer addressId, Integer uid) {
        if (addressId == null) return null;
        EbBarterUserAddress a = addressService.getById(addressId);
        if (a == null || !uid.equals(a.getUid())) return null;
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(a.getProvince())) sb.append(a.getProvince()).append(" ");
        if (StrUtil.isNotBlank(a.getCity())) sb.append(a.getCity()).append(" ");
        if (StrUtil.isNotBlank(a.getDistrict())) sb.append(a.getDistrict()).append(" ");
        if (StrUtil.isNotBlank(a.getDetail())) sb.append(a.getDetail());
        if (StrUtil.isNotBlank(a.getName())) sb.append(" 收件人：").append(a.getName());
        if (StrUtil.isNotBlank(a.getPhone())) sb.append(" ").append(a.getPhone());
        return sb.toString().trim();
    }
}
