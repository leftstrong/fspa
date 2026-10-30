package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.constants.BarterConstants;
import cn.hutool.core.util.StrUtil;
import com.zbkj.common.model.barter.EbBarterApply;
import com.zbkj.common.model.barter.EbBarterEvaluate;
import com.zbkj.common.model.barter.EbBarterItem;
import com.zbkj.common.model.barter.EbBarterMemberLevel;
import com.zbkj.common.model.barter.EbBarterOrder;
import com.zbkj.common.model.barter.EbBarterOrderMessage;
import com.zbkj.common.model.barter.EbBarterUserAddress;
import com.zbkj.common.model.user.User;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.BarterOrderSearchRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.barter.BarterOrderAdminDetailResponse;
import com.zbkj.common.response.barter.BarterOrderDetailResponse;
import com.zbkj.common.vo.LogisticsResultListVo;
import com.zbkj.common.vo.LogisticsResultVo;
import com.zbkj.service.dao.BarterApplyDao;
import com.zbkj.service.dao.BarterItemDao;
import com.zbkj.service.dao.BarterOrderDao;
import com.zbkj.service.service.BarterCreditLogService;
import com.zbkj.service.service.BarterEvaluateService;
import com.zbkj.service.service.BarterConfigService;
import com.zbkj.service.service.BarterMemberLevelService;
import com.zbkj.service.service.BarterMessageService;
import com.zbkj.service.service.BarterNotifyService;
import com.zbkj.service.service.BarterOrderMessageService;
import com.zbkj.service.service.BarterOrderService;
import com.zbkj.service.service.BarterPointsLogService;
import com.zbkj.service.service.BarterUserAddressService;
import com.zbkj.service.service.LogisticService;
import com.zbkj.service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 换物单 接口实现类
 * 核心：complete() 在事务内为双方累加成功交换次数、重算等级、信用+5
 */
@Service("barterOrderService")
public class BarterOrderServiceImpl extends ServiceImpl<BarterOrderDao, EbBarterOrder> implements BarterOrderService {

    @Autowired
    private UserService userService;
    @Autowired
    private BarterMemberLevelService memberLevelService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private BarterNotifyService barterNotifyService;
    @Autowired
    private BarterMessageService barterMessageService;
    @Autowired
    private BarterItemDao barterItemDao;
    @Autowired
    private BarterApplyDao barterApplyDao;
    @Autowired
    private BarterUserAddressService barterUserAddressService;
    /** 平台物流查询通道（一号通 / 阿里云市场，由 logistics_type 决定） */
    @Autowired
    private LogisticService logisticService;
    /** 快递鸟通道客户端：保留备用，当前业务统一走阿里云市场通道，不在调用链上 */
    @Autowired
    private KdniaoExpressClient kdniaoExpressClient;
    @Autowired
    private BarterOrderMessageService barterOrderMessageService;
    /** 换物积分：环解散时退还发起人的成环积分 */
    @Autowired
    private BarterPointsLogService pointsLogService;
    @Autowired
    private BarterCreditLogService creditLogService;
    @Autowired
    private BarterConfigService barterConfigService;
    @Autowired
    private BarterEvaluateService barterEvaluateService;

    /** 物品状态：1在售 2审核中 3驳回(审核未过) 4已交换 5交换中(建单即锁定) 6已下架 */
    private static final int ITEM_STATUS_ON_SALE = 1;
    private static final int ITEM_STATUS_EXCHANGED = 4;
    private static final int ITEM_STATUS_EXCHANGING = 5;

    @Override
    public CommonPage<BarterOrderDetailResponse> myList(PageParamRequest pageParamRequest, Integer status, Integer role) {
        Integer uid = userService.getUserIdException();
        Page<Object> startPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        QueryWrapper<EbBarterOrder> qw = new QueryWrapper<>();
        qw.and(w -> w.eq("owner_uid", uid).or().eq("apply_uid", uid));
        // role: 0=我发起(apply) 1=我收到(owner)
        if (role != null) {
            if (role == 0) qw.eq("apply_uid", uid);
            else if (role == 1) qw.eq("owner_uid", uid);
        }
        if (status != null) qw.eq("status", status);
        qw.orderByDesc("id");
        List<EbBarterOrder> orders = baseMapper.selectList(qw);
        List<BarterOrderDetailResponse> vos = new ArrayList<>();
        for (EbBarterOrder o : orders) vos.add(buildDetailVo(o, uid));
        return CommonPage.restPage(CommonPage.copyPageInfo(startPage, vos));
    }

    @Override
    public CommonPage<EbBarterOrder> adminList(BarterOrderSearchRequest request, PageParamRequest pageParamRequest) {
        Page<Object> startPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        QueryWrapper<EbBarterOrder> qw = new QueryWrapper<>();
        if (request != null) {
            if (request.getStatus() != null) qw.eq("status", request.getStatus());
            if (StrUtil.isNotBlank(request.getOrderSn())) qw.like("order_sn", request.getOrderSn().trim());
            if (request.getOwnerUid() != null) qw.eq("owner_uid", request.getOwnerUid());
            if (request.getApplyUid() != null) qw.eq("apply_uid", request.getApplyUid());
            // 后台「按用户查」：物主或申请人任一方命中即可，运营不需要先判断这人站在哪一侧
            if (request.getLinkUid() != null) {
                Integer link = request.getLinkUid();
                qw.and(w -> w.eq("owner_uid", link).or().eq("apply_uid", link));
            }
            if (StrUtil.isNotBlank(request.getDateLimit())) {
                String[] range = request.getDateLimit().split(",");
                if (range.length == 2) {
                    qw.between("create_time", range[0].trim() + " 00:00:00", range[1].trim() + " 23:59:59");
                }
            }
        }
        qw.orderByDesc("id");
        return CommonPage.restPage(CommonPage.copyPageInfo(startPage, baseMapper.selectList(qw)));
    }

    @Override
    public BarterOrderDetailResponse detail(Integer orderId) {
        EbBarterOrder o = baseMapper.selectById(orderId);
        if (o == null) return null;
        Integer uid = userService.getUserIdException();
        boolean isOwner = o.getOwnerUid().equals(uid);
        // 仅换物双方可见
        if (!isOwner && !o.getApplyUid().equals(uid)) return null;
        return buildDetailVo(o, uid);
    }

    /** 装配换物单详情视图（以「我」的视角展开），供 detail 与 myList 复用 */
    private BarterOrderDetailResponse buildDetailVo(EbBarterOrder o, Integer uid) {
        boolean isOwner = o.getOwnerUid().equals(uid);
        BarterOrderDetailResponse vo = new BarterOrderDetailResponse();
        vo.setId(o.getId());
        vo.setOrderSn(o.getOrderSn());
        vo.setApplyId(o.getApplyId());
        vo.setStatus(o.getStatus());
        vo.setStatusText(orderStatusText(o.getStatus()));
        vo.setDeliveryType(o.getDeliveryType());
        vo.setDeliveryTypeText(o.getDeliveryType() != null && o.getDeliveryType() == 2 ? "快递" : "自提");
        vo.setSelfTakeAddress(o.getSelfTakeAddress());
        vo.setMyRole(isOwner ? "owner" : "apply");
        vo.setPeerRoleText(isOwner ? "申请人" : "物主");
        vo.setItem(toSimpleItem(barterItemDao.selectById(o.getItemId())));
        vo.setOfferItems(toSimpleItems(o.getOfferItemIds()));
        vo.setOwnerUser(toSimpleUser(o.getOwnerUid()));
        vo.setApplyUser(toSimpleUser(o.getApplyUid()));
        String applyMessage = o.getApplyMessage();
        EbBarterApply apply = o.getApplyId() != null ? barterApplyDao.selectById(o.getApplyId()) : null;
        if (applyMessage == null && apply != null) applyMessage = apply.getMessage();
        vo.setApplyMessage(applyMessage);
        vo.setCreateTime(o.getCreateTime());
        vo.setCompleteTime(o.getCompleteTime());
        // 智能换物环标识：让参与者知道这张单不是普通 1v1，而是 N 人接力的一环
        vo.setChainNo(o.getChainNo());
        vo.setChainSeq(o.getChainSeq());
        vo.setChainSize(o.getChainSize());

        // 以「我」的视角展开发货/物流/地址/收货
        Integer myDeliver = isOwner ? o.getOwnerDeliverStatus() : o.getApplyDeliverStatus();
        Integer peerDeliver = isOwner ? o.getApplyDeliverStatus() : o.getOwnerDeliverStatus();
        vo.setMyDeliverStatus(myDeliver);
        vo.setPeerDeliverStatus(peerDeliver);
        vo.setMyLogisticsCompany(isOwner ? o.getOwnerLogisticsCompany() : o.getApplyLogisticsCompany());
        vo.setMyLogisticsCompanyName(BarterConstants.expressName(isOwner ? o.getOwnerLogisticsCompany() : o.getApplyLogisticsCompany()));
        vo.setMyLogisticsNo(isOwner ? o.getOwnerLogisticsNo() : o.getApplyLogisticsNo());
        vo.setPeerLogisticsCompanyName(BarterConstants.expressName(isOwner ? o.getApplyLogisticsCompany() : o.getOwnerLogisticsCompany()));
        vo.setPeerLogisticsNo(isOwner ? o.getApplyLogisticsNo() : o.getOwnerLogisticsNo());
        vo.setMyAddress(isOwner ? o.getOwnerAddress() : o.getApplyAddress());
        vo.setPeerAddress(isOwner ? o.getApplyAddress() : o.getOwnerAddress());
        vo.setMyConfirmRecv(isOwner ? o.getOwnerConfirmRecv() : o.getApplyConfirmRecv());
        vo.setPeerConfirmRecv(isOwner ? o.getApplyConfirmRecv() : o.getOwnerConfirmRecv());

        boolean isPickup = o.getDeliveryType() != null && o.getDeliveryType() == 1;
        int st = o.getStatus() == null ? -1 : o.getStatus();
        // 快递：进行中(2)可发货；待收货(3)且对方已发、我未确认->可确认
        // 自提：进行中(2)且我未确认->可确认（无物流）
        vo.setCanDeliver(st == 2 && !isPickup && (myDeliver == null || myDeliver == 0));
        boolean canConfirmExpress = st == 3 && peerDeliver != null && peerDeliver == 1
                && (isOwner ? o.getOwnerConfirmRecv() : o.getApplyConfirmRecv()) == 0;
        boolean canConfirmPickup = st == 2 && isPickup
                && (isOwner ? o.getOwnerConfirmRecv() : o.getApplyConfirmRecv()) == 0;
        vo.setCanConfirm(canConfirmExpress || canConfirmPickup);
        vo.setCanAccept(isOwner && st == 0);
        vo.setCanReject(isOwner && st == 0);

        // 取消：待确认随便取消；进行中必须双方都还没发货。
        // 一方已寄出后再取消，货在路上而订单消失，是履约事故，只能走纠纷。
        boolean myShipped = myDeliver != null && myDeliver == 1;
        boolean peerShipped = peerDeliver != null && peerDeliver == 1;
        boolean anyShipped = myShipped || peerShipped;
        vo.setCanCancel(st == 0 || (st == 2 && !anyShipped));
        if (st == 2 && anyShipped) {
            vo.setCancelBlockedTip(myShipped && !peerShipped
                    ? "你已发货，不能直接取消。若对方迟迟不发货，可发起纠纷由平台介入"
                    : (peerShipped && !myShipped
                    ? "对方已发货，不能直接取消。收到后确认即可；有问题请发起纠纷"
                    : "双方都已发货，不能取消，请在收到后确认"));
        } else if (st == 3) {
            vo.setCancelBlockedTip("双方都已发货，已完成发货环节，请在收到后确认");
        }
        vo.setCanDispute(st == 2 || st == 3);

        // 改单号：只对自己那一侧、已发货、发货后 N 小时内、最多 M 次（N/M 后台可配）
        int editHours = barterConfigService.expressEditHours();
        int editMax = barterConfigService.expressEditMax();
        boolean canEdit = false;
        String editTip = null;
        if (!isPickup && myShipped && (st == 2 || st == 3)) {
            java.util.Date shippedAt = isOwner ? o.getOwnerDeliverTime() : o.getApplyDeliverTime();
            Integer usedObj = isOwner ? o.getOwnerLogisticsEdit() : o.getApplyLogisticsEdit();
            int used = usedObj == null ? 0 : usedObj;
            if (shippedAt == null) {
                editTip = "没有记录到发货时间，如需修改请联系客服";
            } else {
                long leftMs = editHours * 3600_000L
                        - (System.currentTimeMillis() - shippedAt.getTime());
                if (leftMs <= 0) {
                    editTip = "已超过发货后 " + editHours + " 小时，改单号请联系客服";
                } else if (used >= editMax) {
                    editTip = "已修改 " + used + " 次，达到上限，请联系客服";
                } else {
                    canEdit = true;
                    long hours = Math.max(1, leftMs / 3600_000L);
                    editTip = "发货后 " + editHours + " 小时内可自行修改，还剩约 "
                            + hours + " 小时（还可改 " + (editMax - used) + " 次）";
                }
            }
        }
        vo.setCanUpdateExpress(canEdit);
        vo.setExpressEditTip(editTip);
        return vo;
    }

    /**
     * 根据物品 id 反查它所在的换物单 id（供「已交换商品→查看换物单明细」跳转）
     * 优先返回已完成(4)的单；找不到已完成则返回最近一笔涉及该物品的单
     * 仅返回当前用户参与的换物单
     */
    @Override
    public Integer getOrderIdByItem(Integer itemId) {
        if (itemId == null) return null;
        Integer uid = userService.getUserIdException();
        QueryWrapper<EbBarterOrder> qw = new QueryWrapper<>();
        qw.and(w -> w.eq("owner_uid", uid).or().eq("apply_uid", uid));
        qw.orderByDesc("id");
        List<EbBarterOrder> list = baseMapper.selectList(qw);
        EbBarterOrder completed = null;
        EbBarterOrder any = null;
        String sid = String.valueOf(itemId);
        for (EbBarterOrder o : list) {
            boolean match = o.getItemId() != null && o.getItemId().equals(itemId);
            if (!match && o.getOfferItemIds() != null) {
                for (String s : o.getOfferItemIds().split(",")) {
                    if (s.trim().equals(sid)) { match = true; break; }
                }
            }
            if (!match) continue;
            if (any == null) any = o;
            if (o.getStatus() == 4 && completed == null) completed = o;
        }
        if (completed != null) return completed.getId();
        if (any != null) return any.getId();
        return null;
    }

    private String orderStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
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

    private BarterOrderDetailResponse.SimpleItem toSimpleItem(EbBarterItem item) {
        if (item == null) return null;
        BarterOrderDetailResponse.SimpleItem s = new BarterOrderDetailResponse.SimpleItem();
        s.setId(item.getId());
        s.setTitle(item.getTitle());
        s.setImages(item.getImages());
        s.setCover(item.getImages() == null ? "" : item.getImages().split(",")[0]);
        s.setCity(item.getCity());
        s.setConditionLevel(item.getConditionLevel());
        s.setConditionText(conditionText(item.getConditionLevel()));
        return s;
    }

    private List<BarterOrderDetailResponse.SimpleItem> toSimpleItems(String ids) {
        List<BarterOrderDetailResponse.SimpleItem> list = new ArrayList<>();
        if (ids == null || ids.trim().isEmpty()) return list;
        for (String id : ids.split(",")) {
            if (id.trim().isEmpty()) continue;
            EbBarterItem it = barterItemDao.selectById(id.trim());
            if (it != null) list.add(toSimpleItem(it));
        }
        return list;
    }

    private BarterOrderDetailResponse.SimpleUser toSimpleUser(Integer uid) {
        if (uid == null) return null;
        BarterOrderDetailResponse.SimpleUser u = new BarterOrderDetailResponse.SimpleUser();
        u.setUid(uid);
        User user = userService.getById(uid);
        if (user != null) {
            u.setNickname(user.getNickname());
            u.setAvatar(user.getAvatar());
            u.setExchangeCount(user.getBarterExchangeCount());
            u.setCreditScore(user.getCreditScore());
            u.setLevelId(user.getBarterLevelId());
        }
        return u;
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

    @Override
    public Boolean deliver(Integer orderId, String role, String logisticsCompany, String logisticsNo) {
        EbBarterOrder order = baseMapper.selectById(orderId);
        if (order == null) throw new CrmebException("换物单不存在");
        if (order.getStatus() != 2) throw new CrmebException("当前状态不可发货，订单可能已取消或已完成"); // 需进行中(待发货)
        // 快递交付必须选快递公司 + 填物流单号
        if (order.getDeliveryType() != null && order.getDeliveryType() == 2) {
            if (logisticsNo == null || logisticsNo.trim().isEmpty()) throw new CrmebException("请填写快递单号");
            if (logisticsCompany == null || logisticsCompany.trim().isEmpty()) throw new CrmebException("请选择快递公司");
            logisticsNo = logisticsNo.trim().replaceAll("[\\s-]", "");
            logisticsCompany = logisticsCompany.trim();
            // 快递单号只有字母和数字，长度 6-32；拦住随手乱填
            if (!logisticsNo.matches("^[A-Za-z0-9]{6,32}$")) throw new CrmebException("快递单号格式不对，请核对后重新输入");
        }
        if ("owner".equals(role)) {
            order.setOwnerDeliverStatus(1);
            order.setOwnerDeliverTime(new Date()); // 发货时间：用于 24 小时内自查改单号
            if (logisticsNo != null) order.setOwnerLogisticsNo(logisticsNo);
            if (logisticsCompany != null) order.setOwnerLogisticsCompany(logisticsCompany);
        } else if ("apply".equals(role)) {
            order.setApplyDeliverStatus(1);
            order.setApplyDeliverTime(new Date());
            if (logisticsNo != null) order.setApplyLogisticsNo(logisticsNo);
            if (logisticsCompany != null) order.setApplyLogisticsCompany(logisticsCompany);
        } else {
            throw new CrmebException("无权操作该换物单");
        }
        // 双方都交付则进入待收货
        if (order.getOwnerDeliverStatus() == 1 && order.getApplyDeliverStatus() == 1) {
            order.setStatus(3);
        }
        order.setUpdateTime(new Date());
        Boolean ok = transactionTemplate.execute(e -> {
            baseMapper.updateById(order);
            return Boolean.TRUE;
        });
        if (ok) {
            EbBarterItem item = barterItemDao.selectById(order.getItemId());
            String title = item != null ? item.getTitle() : "-";
            Integer target = "owner".equals(role) ? order.getApplyUid() : order.getOwnerUid();
            String expName = cn.hutool.core.util.StrUtil.isNotBlank(logisticsCompany)
                    ? BarterConstants.expressName(logisticsCompany) : "";
            barterMessageService.push(target, BarterConstants.MSG_ORDER_DELIVERED, order.getId(),
                    "对方已发货", "《" + title + "》对方已发货" + (expName.isEmpty() ? "" : "（" + expName + "）") + "，请注意查收");
            barterNotifyService.notifyDelivered(target, title, role);
        }
        return ok;
    }

    /**
     * 发货后修改快递单号。
     * 填错单号是最高频的履约事故（对方照着错单号查不到物流会直接投诉），
     * 所以给本人一个 24 小时、最多 3 次的自查窗口；超出窗口转人工。
     * 只允许改自己那一侧，且订单必须还在履约中（进行中 / 待收货）。
     */
    @Override
    public Boolean updateExpress(Integer orderId, String role, String logisticsCompany, String logisticsNo) {
        EbBarterOrder order = baseMapper.selectById(orderId);
        if (order == null) throw new CrmebException("换物单不存在");
        boolean isOwner = "owner".equals(role);
        if (!isOwner && !"apply".equals(role)) throw new CrmebException("无权操作该换物单");

        Integer uid = userService.getUserIdException();
        if (isOwner ? !order.getOwnerUid().equals(uid) : !order.getApplyUid().equals(uid)) {
            throw new CrmebException("无权修改对方的快递单号");
        }
        if (order.getDeliveryType() == null || order.getDeliveryType() != 2) {
            throw new CrmebException("自提单没有快递单号");
        }
        Integer st = order.getStatus();
        if (st == null || (st != 2 && st != 3)) {
            throw new CrmebException("订单已结束，不能再修改单号");
        }
        Integer myDeliver = isOwner ? order.getOwnerDeliverStatus() : order.getApplyDeliverStatus();
        if (myDeliver == null || myDeliver != 1) throw new CrmebException("你还没有标记发货，无需修改单号");

        Date deliveredAt = isOwner ? order.getOwnerDeliverTime() : order.getApplyDeliverTime();
        if (deliveredAt == null) throw new CrmebException("没有记录到发货时间，请联系客服处理");
        long passedMs = System.currentTimeMillis() - deliveredAt.getTime();
        int editHoursCfg = barterConfigService.expressEditHours();
        int editMaxCfg = barterConfigService.expressEditMax();
        long limitMs = editHoursCfg * 3600_000L;
        if (passedMs > limitMs) {
            throw new CrmebException("已超过发货后 " + editHoursCfg
                    + " 小时，不能自行修改单号，请联系对方说明或找客服处理");
        }

        Integer used = isOwner ? order.getOwnerLogisticsEdit() : order.getApplyLogisticsEdit();
        int editedCount = used == null ? 0 : used;
        if (editedCount >= editMaxCfg) {
            throw new CrmebException("单号最多修改 " + editMaxCfg + " 次，已达上限，请联系客服");
        }

        if (logisticsNo == null || logisticsNo.trim().isEmpty()) throw new CrmebException("请填写快递单号");
        if (logisticsCompany == null || logisticsCompany.trim().isEmpty()) throw new CrmebException("请选择快递公司");
        String newNo = logisticsNo.trim().replaceAll("[\\s-]", "");
        String newCompany = logisticsCompany.trim();
        if (!newNo.matches("^[A-Za-z0-9]{6,32}$")) throw new CrmebException("快递单号格式不对，请核对后重新输入");

        String oldNo = isOwner ? order.getOwnerLogisticsNo() : order.getApplyLogisticsNo();
        if (newNo.equals(oldNo) && newCompany.equals(isOwner ? order.getOwnerLogisticsCompany() : order.getApplyLogisticsCompany())) {
            throw new CrmebException("新单号和原来一样，无需修改");
        }

        if (isOwner) {
            order.setOwnerLogisticsNo(newNo);
            order.setOwnerLogisticsCompany(newCompany);
            order.setOwnerLogisticsEdit(editedCount + 1);
        } else {
            order.setApplyLogisticsNo(newNo);
            order.setApplyLogisticsCompany(newCompany);
            order.setApplyLogisticsEdit(editedCount + 1);
        }
        order.setUpdateTime(new Date());

        Boolean ok = transactionTemplate.execute(e -> {
            baseMapper.updateById(order);
            return Boolean.TRUE;
        });
        if (Boolean.TRUE.equals(ok)) {
            Integer target = isOwner ? order.getApplyUid() : order.getOwnerUid();
            EbBarterItem item = barterItemDao.selectById(order.getItemId());
            String title = item != null ? item.getTitle() : "-";
            barterMessageService.push(target, BarterConstants.MSG_ORDER_DELIVERED, order.getId(),
                    "对方更新了快递单号",
                    "《" + title + "》的发货单号已由 " + (oldNo == null || oldNo.isEmpty() ? "-" : oldNo)
                            + " 改为 " + newNo + "（" + BarterConstants.expressName(newCompany) + "），请按新单号查收");
        }
        return ok;
    }

    @Override
    public Boolean confirmRecv(Integer orderId, String role) {
        EbBarterOrder order = baseMapper.selectById(orderId);
        if (order == null) return false;
        boolean isPickup = order.getDeliveryType() != null && order.getDeliveryType() == 1;
        // 快递需待收货(3)；自提需进行中(2)且对方无需发货
        if (!isPickup && order.getStatus() != 3) return false;
        if (isPickup && order.getStatus() != 2) return false;
        if ("owner".equals(role)) order.setOwnerConfirmRecv(1);
        else if ("apply".equals(role)) order.setApplyConfirmRecv(1);
        else return false;
        order.setUpdateTime(new Date());
        final EbBarterOrder fOrder = order;
        final String fRole = role;
        return transactionTemplate.execute(e -> {
            baseMapper.updateById(fOrder);
            EbBarterItem item = barterItemDao.selectById(fOrder.getItemId());
            String title2 = item != null ? item.getTitle() : "-";
            Integer peer = "owner".equals(fRole) ? fOrder.getApplyUid() : fOrder.getOwnerUid();
            barterMessageService.push(peer, BarterConstants.MSG_ORDER_CONFIRMED, fOrder.getId(),
                    "对方已确认收货", "《" + title2 + "》对方已确认收货");
            // 双方都确认收货 -> 完成
            if (fOrder.getOwnerConfirmRecv() == 1 && fOrder.getApplyConfirmRecv() == 1) {
                complete(fOrder.getId());
            }
            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean autoCompleteBySystem(Integer orderId) {
        EbBarterOrder order = baseMapper.selectById(orderId);
        if (order == null || order.getStatus() != 3) return false; // 仅待收货(3)可系统完成
        order.setOwnerConfirmRecv(1);
        order.setApplyConfirmRecv(1);
        order.setUpdateTime(new Date());
        final EbBarterOrder fOrder = order;
        return transactionTemplate.execute(e -> {
            baseMapper.updateById(fOrder);
            complete(fOrder.getId());
            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean complete(Integer orderId) {
        EbBarterOrder order = baseMapper.selectById(orderId);
        if (order == null) return false;
        if (!(order.getOwnerConfirmRecv() == 1 && order.getApplyConfirmRecv() == 1)) return false;
        if (order.getStatus() == 4) return true; // 已完成直接返回
        return transactionTemplate.execute(e -> {
            order.setStatus(4); // 已完成
            order.setCompleteTime(new Date());
            order.setUpdateTime(new Date());
            baseMapper.updateById(order);
            incrUser(order.getOwnerUid(), order.getId());
            incrUser(order.getApplyUid(), order.getId());
            EbBarterItem item = barterItemDao.selectById(order.getItemId());
            String title = item != null ? item.getTitle() : "-";
            // 路径规划费：只在成交这一刻结算；被拒/取消/超时都不收费，因此没有退款链路
            settlePathFee(order);
            // 接力过户：双方各自把「收到的这件」落到自己名下，状态为已交换(4)。
            // 不转移所有权的话，用户走完这一棒只拿到一个终态记录，没法拿它去换下一件。
            List<Integer> itemIds = new ArrayList<>();
            if (order.getItemId() != null) itemIds.add(order.getItemId());
            if (StrUtil.isNotBlank(order.getOfferItemIds())) {
                for (String s : order.getOfferItemIds().split(",")) {
                    try { itemIds.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignore) {}
                }
            }
            for (Integer iid : itemIds) {
                EbBarterItem it = barterItemDao.selectById(iid);
                if (it != null) {
                    // itemId 这件是物主给出的，归申请人；其余的（我的付出）归物主
                    Integer newOwner = iid.equals(order.getItemId()) ? order.getApplyUid() : order.getOwnerUid();
                    it.setUid(newOwner);
                    it.setStatus(ITEM_STATUS_EXCHANGED);
                    it.setTopEndTime(null);
                    it.setReceiveFromOrderId(order.getId());
                    it.setUpdateTime(new Date());
                    barterItemDao.updateById(it);
                }
            }
            barterMessageService.push(order.getOwnerUid(), BarterConstants.MSG_ORDER_COMPLETED, order.getId(),
                    "换物已完成", "《" + title + "》换物完成，积分已到账，记得互评哦");
            barterMessageService.push(order.getApplyUid(), BarterConstants.MSG_ORDER_COMPLETED, order.getId(),
                    "换物已完成", "《" + title + "》换物完成，积分已到账，记得互评哦");
            barterNotifyService.notifyCompleted(order.getOwnerUid(), title);
            barterNotifyService.notifyCompleted(order.getApplyUid(), title);
            // 接到手的这件如果要继续换，给个明确入口（用户点一下才上架，避免不想要的东西被挂到在售里）
            if (StrUtil.isNotBlank(order.getChainNo()) && order.getChainSize() != null && order.getChainSize() > 2) {
                barterMessageService.push(order.getApplyUid(), BarterConstants.MSG_ORDER_COMPLETED, order.getId(),
                        "这一棒走完了", "《" + title + "》已经到你名下。想继续走下一棒，去「我的物品」点「继续换下一件」");
            }
            return Boolean.TRUE;
        });
    }

    /**
     * 路径规划费结算（成交时）。
     * 发起时已冻结 pathFeeFrozen，这里只把冻结转实扣（不再动可用余额）；
     * 冻结不足应收的部分免收 —— 不让用户变负分，也不因差几分卡住一笔已完成的换物。
     * 兼容老单：没有冻结记录的，退回按当时可用余额扣。
     */
    private void settlePathFee(EbBarterOrder order) {
        Integer fee = order.getPathFeePoints();
        if (fee == null || fee <= 0) return;
        if (order.getPathFeePaid() != null && order.getPathFeePaid() == 1) return;
        int frozen = order.getPathFeeFrozen() == null ? 0 : order.getPathFeeFrozen();
        int charge;
        if (frozen > 0) {
            charge = Math.min(frozen, fee);
            if (charge > 0) {
                pointsLogService.settleFrozenPoints(order.getApplyUid(), charge,
                        "智能换物路径规划费 " + fee + " 分（成交结算，由冻结转入）", order.getId());
            }
        } else {
            User payer = userService.getById(order.getApplyUid());
            int bal = payer == null || payer.getBarterPoints() == null ? 0 : payer.getBarterPoints();
            charge = Math.min(Math.max(bal, 0), fee);
            if (charge > 0) {
                pointsLogService.changePoints(order.getApplyUid(), 2, charge,
                        "智能换物路径规划费 " + fee + " 分（成交结算）", 5, order.getId());
            }
        }
        order.setPathFeeCharged(charge);
        order.setPathFeePaid(1);
        baseMapper.updateById(order);
        if (charge < fee) {
            barterMessageService.push(order.getApplyUid(), BarterConstants.MSG_ORDER_COMPLETED, order.getId(),
                    "路径费已结算", "路径规划费应收 " + fee + " 分，本次实扣 " + charge + " 分，剩余部分免收");
        }
    }

    /** 未结算的冻结路径费原路解冻（谁发起退给谁），返回解冻金额 */
    private int unfreezeIfAny(EbBarterOrder order, String mark) {
        Integer fz = order.getPathFeeFrozen();
        if (fz == null || fz <= 0) return 0;
        if (order.getPathFeePaid() != null && order.getPathFeePaid() == 1) return 0; // 已成交转实扣，不退
        Integer uid = order.getApplyUid();
        if (uid == null) return 0;
        Integer bal = pointsLogService.unfreezePoints(uid, fz, mark + " " + fz + " 分", order.getId());
        if (bal == null) return 0;
        order.setPathFeeFrozen(0);
        return fz;
    }

    /** 换物单关闭（拒绝/取消）时，将涉及物品从「交换中(5)」释放回「在售(1)」；已交换(4)的物品不动 */
    private void releaseItemsToOnSale(EbBarterOrder order) {
        List<Integer> ids = new ArrayList<>();
        if (order.getItemId() != null) ids.add(order.getItemId());
        if (StrUtil.isNotBlank(order.getOfferItemIds())) {
            for (String s : order.getOfferItemIds().split(",")) {
                try { ids.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignore) {}
            }
        }
        for (Integer iid : ids) {
            EbBarterItem it = barterItemDao.selectById(iid);
            if (it != null && it.getStatus() != null && it.getStatus() == ITEM_STATUS_EXCHANGING) {
                it.setStatus(ITEM_STATUS_ON_SALE);
                it.setUpdateTime(new Date());
                barterItemDao.updateById(it);
            }
        }
    }

    /** 为单个用户累加成功交换次数、重算等级、信用+5 */
    private void incrUser(Integer uid, Integer orderId) {
        User user = userService.getById(uid);
        if (user == null) return;
        int cnt = (user.getBarterExchangeCount() == null ? 0 : user.getBarterExchangeCount()) + 1;
        user.setBarterExchangeCount(cnt);
        Integer levelId = memberLevelService.matchLevelId(cnt);
        user.setBarterLevelId(levelId);
        // 信用分交由 creditLogService 统一加（含流水留痕）；失败时降级为直接改余额，
        // 两处只能走一条，否则会重复加分。加分值后台可配（barter_credit_per_deal）
        int creditDelta = barterConfigService.creditPerDeal();
        try {
            creditLogService.changeCredit(uid, creditDelta,
                    "换物完成，信用 +" + creditDelta, 1, orderId);
        } catch (Exception ignore) {
            int credit = (user.getCreditScore() == null ? 0 : user.getCreditScore()) + creditDelta;
            user.setCreditScore(credit);
            userService.updateById(user);
        }
        // 换物成交奖励积分：双方各得，作为积分的正向来源（原来只有「退款」会回补，属只出不进）
        int reward = barterConfigService.pointsPerDeal();
        pointsLogService.changePoints(uid, 1, reward, "换物成功奖励 " + reward + " 积分", 4, orderId);
        // 同步刷新本地缓存等级对象（如需）
        List<EbBarterMemberLevel> all = memberLevelService.tree();
        for (EbBarterMemberLevel lv : all) {
            if (lv.getId().equals(levelId)) break;
        }
    }

    @Override
    public Boolean cancel(Integer orderId, String reason) {
        EbBarterOrder order = baseMapper.selectById(orderId);
        if (order == null) throw new CrmebException("换物单不存在");
        Integer st = order.getStatus();
        if (st != null && (st == 1 || st == 4)) return true; // 已是终态，幂等返回
        String msg = StrUtil.isNotBlank(reason) ? reason : "平台已介入，该换物单已取消";
        order.setStatus(1); // 已取消
        order.setUpdateTime(new Date());
        // 环单走整环解散：会一并释放物品、退还成环积分、通知环上所有人
        if (StrUtil.isNotBlank(order.getChainNo())) {
            return dissolveChain(order, 1, msg + "，换物环已解散、物品已恢复在售");
        }
        return transactionTemplate.execute(e -> {
            baseMapper.updateById(order);
            // 后台取消最容易漏的一步：不释放物品，它会永久卡在「交换中」不可再换
            releaseItemsToOnSale(order);
            Set<Integer> uids = new LinkedHashSet<>();
            if (order.getOwnerUid() != null) uids.add(order.getOwnerUid());
            if (order.getApplyUid() != null) uids.add(order.getApplyUid());
            for (Integer u : uids) {
                barterMessageService.push(u, BarterConstants.MSG_ORDER_CANCELLED, order.getId(),
                        "换物单已被平台取消", msg);
            }
            return Boolean.TRUE;
        });
    }

    /* ---------------- 新流程：申请页直接生成换物单 ---------------- */

    @Override
    public Integer createOrder(Integer targetItemId, Integer offerItemId, String applyMessage, Integer addressId) {
        Integer uid = userService.getUserIdException();
        EbBarterItem target = barterItemDao.selectById(targetItemId);
        if (target == null || target.getStatus() == null || target.getStatus() != 1) return -1; // 物品不存在或未在售
        if (target.getUid().equals(uid)) return -1; // 不能和自己换
        if (target.getDeliveryType() == null) return -1; // 物品未设置交付方式
        EbBarterItem offer = barterItemDao.selectById(offerItemId);
        if (offer == null || !offer.getUid().equals(uid) || offer.getStatus() == null || offer.getStatus() != 1) return -1; // 提供的物品必须是自己在售

        boolean isPickup = target.getDeliveryType() == 1;
        String applyAddr = null;
        if (!isPickup) {
            // 快递：申请人（我）必须已设置默认收货地址
            if (!hasDefaultAddress(uid)) return -3;
            // 快递：必须提供申请人收货地址快照
            if (addressId == null) return -1;
            applyAddr = addressText(addressId);
            if (applyAddr == null) return -1;
            // 快递：被申请人（物主）也必须已设置默认收货地址，否则无法发起
            if (!hasDefaultAddress(target.getUid())) return -2;
        }
        final EbBarterItem fTarget = target;
        final EbBarterItem fOffer = offer;
        final boolean fPickup = isPickup;
        final String fApplyAddr = applyAddr;
        final Integer fAddrId = addressId;
        EbBarterOrder order = new EbBarterOrder();
        order.setOrderSn(System.currentTimeMillis() + "" + uid);
        order.setItemId(fTarget.getId());
        order.setOwnerUid(fTarget.getUid());
        order.setApplyUid(uid);
        order.setOfferItemIds(String.valueOf(fOffer.getId()));
        order.setStatus(0); // 待确认
        order.setDeliveryType(fTarget.getDeliveryType()); // 自提/快递继承物品设置
        order.setApplyMessage(applyMessage);
        order.setApplyAddress(fApplyAddr);
        order.setApplicantAddressId(fAddrId);
        order.setOwnerDeliverStatus(0);
        order.setApplyDeliverStatus(0);
        order.setOwnerConfirmRecv(0);
        order.setApplyConfirmRecv(0);
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        Integer oid = transactionTemplate.execute(e -> {
            baseMapper.insert(order);
            Integer id = order.getId();
            // 建单即锁定双方物品为「交换中(5)」，避免重复占用 / 他人误判为可换
            fTarget.setStatus(ITEM_STATUS_EXCHANGING);
            fTarget.setUpdateTime(new Date());
            barterItemDao.updateById(fTarget);
            fOffer.setStatus(ITEM_STATUS_EXCHANGING);
            fOffer.setUpdateTime(new Date());
            barterItemDao.updateById(fOffer);
            return id;
        });
        // 通知物品主人：收到新的换物请求
        User applicant = userService.getById(uid);
        String applicantName = applicant != null ? applicant.getNickname() : "有人";
        barterMessageService.push(fTarget.getUid(), BarterConstants.MSG_ORDER_PENDING, oid,
                "收到新的换物请求", applicantName + " 想用 TA 的《" + fOffer.getTitle() + "》换你的《" + fTarget.getTitle() + "》");
        return oid;
    }

    @Override
    public Boolean acceptOrder(Integer orderId) {
        EbBarterOrder order = baseMapper.selectById(orderId);
        if (order == null || order.getStatus() != 0) return false; // 仅待确认可接受
        Integer uid = userService.getUserIdException();
        if (!order.getOwnerUid().equals(uid)) return false;
        boolean isPickup = order.getDeliveryType() != null && order.getDeliveryType() == 1;
        String ownerAddr = null;
        if (!isPickup) {
            ownerAddr = defaultAddressText(order.getOwnerUid());
            if (ownerAddr == null) return false; // 物主无默认地址无法走快递
        }
        final boolean fPickup = isPickup;
        final String fOwnerAddr = ownerAddr;
        final EbBarterOrder fOrder = order;
        return transactionTemplate.execute(e -> {
            fOrder.setStatus(2); // 进行中（快递=待发货 / 自提=待完成）
            fOrder.setOwnerAddress(fOwnerAddr);
            fOrder.setUpdateTime(new Date());
            baseMapper.updateById(fOrder);
            EbBarterItem item = barterItemDao.selectById(fOrder.getItemId());
            String title = item != null ? item.getTitle() : "-";
            // 申请人：请求被接受
            barterMessageService.push(fOrder.getApplyUid(), BarterConstants.MSG_ORDER_ACCEPTED, fOrder.getId(),
                    "换物请求被接受", "对方接受了你发起的换物，请尽快" + (fPickup ? "自提交接" : "发货"));
            // 双方：换物单已进入进行中
            String tip = fPickup ? "请按约定自提点完成交付" : "请使用快递发货并填写物流单号";
            barterMessageService.push(fOrder.getOwnerUid(), BarterConstants.MSG_ORDER_CREATED, fOrder.getId(),
                    "换物单进行中", "《" + title + "》" + tip);
            barterMessageService.push(fOrder.getApplyUid(), BarterConstants.MSG_ORDER_CREATED, fOrder.getId(),
                    "换物单进行中", "《" + title + "》" + tip);
            barterNotifyService.notifyApplyAccepted(fOrder.getApplyUid(), title);
            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean rejectOrder(Integer orderId) {
        EbBarterOrder order = baseMapper.selectById(orderId);
        if (order == null || order.getStatus() != 0) return false;
        Integer uid = userService.getUserIdException();
        if (!order.getOwnerUid().equals(uid)) return false;
        order.setStatus(5); // 已拒绝
        order.setUpdateTime(new Date());
        // 智能换物环：一人退出必须整环解散，否则其他人的物品会被永久锁在「交换中」
        if (StrUtil.isNotBlank(order.getChainNo())) {
            return dissolveChain(order, 5, "有人拒绝了这个换物环，环已解散，你的物品已恢复在售");
        }
        releaseItemsToOnSale(order); // 拒绝后释放双方物品回在售
        EbBarterItem item = barterItemDao.selectById(order.getItemId());
        String t = item != null ? item.getTitle() : "-";
        barterMessageService.push(order.getApplyUid(), BarterConstants.MSG_ORDER_REJECTED, order.getId(),
                "换物请求被拒绝", "很遗憾，对方拒绝了你对《" + t + "》的换物请求");
        return transactionTemplate.execute(e -> {
            baseMapper.updateById(order);
            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean frontCancel(Integer orderId) {
        EbBarterOrder order = baseMapper.selectById(orderId);
        if (order == null) throw new CrmebException("换物单不存在");
        Integer st = order.getStatus();
        if (st == null || (st != 0 && st != 2)) throw new CrmebException("当前状态不可取消"); // 仅待确认/进行中可取消
        Integer uid = userService.getUserIdException();
        if (!order.getOwnerUid().equals(uid) && !order.getApplyUid().equals(uid)) {
            throw new CrmebException("无权操作该换物单");
        }
        // 快递模式下任一方已发货就锁死取消：货已寄出，单方取消会让对方财物两空，只能走纠纷
        if (order.getDeliveryType() != null && order.getDeliveryType() == 2) {
            boolean ownerShipped = order.getOwnerDeliverStatus() != null && order.getOwnerDeliverStatus() == 1;
            boolean applyShipped = order.getApplyDeliverStatus() != null && order.getApplyDeliverStatus() == 1;
            if (ownerShipped || applyShipped) {
                throw new CrmebException("已有快递发货，不能直接取消。如需终止请发起纠纷，由平台介入处理");
            }
        }
        // 环式换物：解散是整环动作，只要环上任何一张单已发货就不能解散，
        // 否则别人寄出的货会被「取消自己那张」连带作废
        if (StrUtil.isNotBlank(order.getChainNo())) {
            List<EbBarterOrder> chainOrders = baseMapper.selectList(
                    new QueryWrapper<EbBarterOrder>().eq("chain_no", order.getChainNo()));
            for (EbBarterOrder co : chainOrders) {
                boolean coShipped = (co.getOwnerDeliverStatus() != null && co.getOwnerDeliverStatus() == 1)
                        || (co.getApplyDeliverStatus() != null && co.getApplyDeliverStatus() == 1);
                if (coShipped) {
                    throw new CrmebException("这个换物环里已经有人发货了，不能解散。如需终止请发起纠纷，由平台介入处理");
                }
            }
        }
        order.setStatus(1); // 已取消
        order.setUpdateTime(new Date());
        // 智能换物环：同上，任一方取消立即整环解散
        if (StrUtil.isNotBlank(order.getChainNo())) {
            return dissolveChain(order, 1, "有人取消了这个换物环，环已解散，你的物品已恢复在售");
        }
        releaseItemsToOnSale(order); // 取消后释放双方物品回在售
        // 兜底：非环单笔若也冻过路径费，取消即解冻
        unfreezeIfAny(order, "换物单已取消，解冻路径规划费");
        Integer peer = order.getOwnerUid().equals(uid) ? order.getApplyUid() : order.getOwnerUid();
        barterMessageService.push(peer, BarterConstants.MSG_ORDER_CANCELLED, order.getId(),
                "换物单已取消", "对方取消了这笔换物");
        return transactionTemplate.execute(e -> {
            baseMapper.updateById(order);
            return Boolean.TRUE;
        });
    }

    /**
     * 智能换物环联动解散：环上任一张单被拒 / 取消时，
     * 把同环其余未完成的单一起取消、释放环上所有物品回在售、通知除操作者外的所有参与者。
     * 不做这一步会出现「一人退出、其他人的物品永久锁在交换中」的死锁。
     */
    private Boolean dissolveChain(EbBarterOrder cause, Integer causeStatus, String reason) {
        String chainNo = cause.getChainNo();
        List<EbBarterOrder> chain = baseMapper.selectList(new QueryWrapper<EbBarterOrder>().eq("chain_no", chainNo));
        if (chain.isEmpty()) chain = new ArrayList<>();
        boolean causedInList = false;
        for (EbBarterOrder o : chain) if (o.getId().equals(cause.getId())) causedInList = true;
        if (!causedInList) chain.add(cause);
        // 后台管理员处理纠纷时没有前台登录态，取不到本人 uid 属正常，此时给所有人群发
        Integer uidTmp = null;
        try {
            uidTmp = userService.getUserIdException();
        } catch (Exception ignore) {
            // ignore
        }
        final Integer uid = uidTmp;
        Date now = new Date();
        final List<EbBarterOrder> members = chain;
        // 首次解散才退积分：环上原本没有任何终态单，说明这次是第一个人退出
        boolean firstDissolve = true;
        for (EbBarterOrder o : members) {
            if (o.getId().equals(cause.getId())) continue;
            Integer s = o.getStatus();
            if (s != null && (s == 1 || s == 4 || s == 5)) {
                firstDissolve = false;
                break;
            }
        }
        // 发起人 = 环上第 1 张单的申请人；成环时只有他付了积分
        Integer payer = null;
        Integer chainSize = cause.getChainSize();
        for (EbBarterOrder o : members) {
            if (o.getChainSeq() != null && o.getChainSeq() == 1) {
                payer = o.getApplyUid();
                if (o.getChainSize() != null) chainSize = o.getChainSize();
                break;
            }
        }
        final Integer fPayer = payer;
        final Integer fSize = chainSize;
        final boolean fFirst = firstDissolve;
        return transactionTemplate.execute(e -> {
            for (EbBarterOrder o : members) {
                if (o.getId().equals(cause.getId())) continue;
                Integer s = o.getStatus();
                if (s != null && (s == 1 || s == 4 || s == 5)) continue; // 已有终态，不重复处理
                o.setStatus(1);
                o.setUpdateTime(now);
                baseMapper.updateById(o);
            }
            cause.setStatus(causeStatus);
            cause.setUpdateTime(now);
            baseMapper.updateById(cause);
            // 释放环上所有物品：交换中(5) → 在售(1)
            for (EbBarterOrder o : members) releaseItemsToOnSale(o);
            // 没走完就不该收路径费：把发起时冻结的积分原路解冻（谁发起退给谁）
            // 老单没有冻结记录但确实扣过费的，仍按已扣金额退款
            String tail = "";
            int refund = 0;
            for (EbBarterOrder o : members) {
                Integer payerUid = o.getApplyUid() != null ? o.getApplyUid()
                        : (fPayer != null ? fPayer : cause.getApplyUid());
                Integer fz = o.getPathFeeFrozen();
                if (fz != null && fz > 0 && (o.getPathFeePaid() == null || o.getPathFeePaid() != 1)) {
                    Integer bal = pointsLogService.unfreezePoints(payerUid, fz,
                            "换物未完成，解冻路径规划费 " + fz + " 分", o.getId());
                    if (bal != null) {
                        refund += fz;
                        o.setPathFeeFrozen(0);
                        baseMapper.updateById(o);
                    }
                } else if (o.getPathFeePaid() != null && o.getPathFeePaid() == 1
                        && o.getPathFeeCharged() != null && o.getPathFeeCharged() > 0) {
                    Integer bal = pointsLogService.changePoints(payerUid, 1, o.getPathFeeCharged(),
                            "换物未完成，退还路径规划费 " + o.getPathFeeCharged() + " 分", 5, o.getId());
                    if (bal != null) refund += o.getPathFeeCharged();
                }
            }
            if (refund > 0) tail = "；冻结的路径规划费 " + refund + " 分已退回";
            // 通知环上所有人（除操作者）
            Set<Integer> uids = new LinkedHashSet<>();
            for (EbBarterOrder o : members) {
                if (o.getOwnerUid() != null) uids.add(o.getOwnerUid());
                if (o.getApplyUid() != null) uids.add(o.getApplyUid());
            }
            for (Integer u : uids) {
                if (uid != null && u.equals(uid)) continue;
                barterMessageService.push(u, BarterConstants.MSG_ORDER_CANCELLED, cause.getId(),
                        "换物未完成", reason + tail);
            }
            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean dispute(Integer orderId) {
        EbBarterOrder order = baseMapper.selectById(orderId);
        if (order == null) return false;
        Integer st = order.getStatus();
        if (st != 2 && st != 3) return false; // 进行中/待收货可发起纠纷
        Integer uid = userService.getUserIdException();
        if (!order.getOwnerUid().equals(uid) && !order.getApplyUid().equals(uid)) return false;
        order.setStatus(6); // 纠纷中
        order.setUpdateTime(new Date());
        Integer peer = order.getOwnerUid().equals(uid) ? order.getApplyUid() : order.getOwnerUid();
        barterMessageService.push(peer, BarterConstants.MSG_ORDER_DISPUTE, order.getId(),
                "对方发起纠纷", "对方对这笔换物发起了纠纷，请关注处理");
        return transactionTemplate.execute(e -> {
            baseMapper.updateById(order);
            return Boolean.TRUE;
        });
    }

    @Override
    public List<EbBarterOrderMessage> orderMessages(Integer orderId) {
        EbBarterOrder order = baseMapper.selectById(orderId);
        if (order == null) return new ArrayList<>();
        Integer uid = userService.getUserIdException();
        if (!order.getOwnerUid().equals(uid) && !order.getApplyUid().equals(uid)) return new ArrayList<>();
        return barterOrderMessageService.listByOrder(orderId);
    }

    /** 取用户某地址的快照文本 */
    private String addressText(Integer addressId) {
        if (addressId == null) return null;
        EbBarterUserAddress a = barterUserAddressService.getById(addressId);
        if (a == null) return null;
        StringBuilder sb = new StringBuilder();
        if (a.getName() != null) sb.append(a.getName()).append(' ');
        if (a.getPhone() != null) sb.append(a.getPhone()).append(' ');
        if (a.getProvince() != null) sb.append(a.getProvince());
        if (a.getCity() != null) sb.append(a.getCity());
        if (a.getDistrict() != null) sb.append(a.getDistrict());
        if (a.getDetail() != null) sb.append(a.getDetail());
        return sb.length() == 0 ? null : sb.toString();
    }

    /** 取用户默认收货地址的快照文本，无地址返回 null */
    private String defaultAddressText(Integer uid) {
        if (uid == null) return null;
        List<EbBarterUserAddress> list = barterUserAddressService.myList(uid);
        if (list == null || list.isEmpty()) return null;
        EbBarterUserAddress a = list.get(0); // myList 默认地址优先
        StringBuilder sb = new StringBuilder();
        if (a.getName() != null) sb.append(a.getName()).append(' ');
        if (a.getPhone() != null) sb.append(a.getPhone()).append(' ');
        if (a.getProvince() != null) sb.append(a.getProvince());
        if (a.getCity() != null) sb.append(a.getCity());
        if (a.getDistrict() != null) sb.append(a.getDistrict());
        if (a.getDetail() != null) sb.append(a.getDetail());
        return sb.length() == 0 ? null : sb.toString();
    }

    /** 用户是否设置了默认收货地址（is_default=1）：快递换物时申请人与物主都必须满足 */
    private boolean hasDefaultAddress(Integer uid) {
        if (uid == null) return false;
        List<EbBarterUserAddress> list = barterUserAddressService.myList(uid);
        if (list == null || list.isEmpty()) return false;
        return list.stream().anyMatch(a -> a.getIsDefault() != null && a.getIsDefault() == 1);
    }

    /**
     * 从收货地址文本里取手机号后四位（顺丰类快递查询必填）。
     * 换物单地址串形如「默认 13800001001 广东省深圳市南山区xxx路1号」。
     */
    private String matchPhoneTail(String address) {
        if (StrUtil.isBlank(address)) return null;
        Matcher m = Pattern.compile("1[3-9]\\d{9}").matcher(address);
        if (!m.find()) return null;
        String phone = m.group();
        return phone.length() >= 4 ? phone.substring(phone.length() - 4) : null;
    }

    /**
     * 查询换物单某一方的快递轨迹。
     * 复用平台已有的物流查询通道（LogisticService：一号通 / 阿里云市场二选一）。
     * 任何异常都降级为 available=false + tip，不让详情页因物流挂掉。
     */
    @Override
    public Map<String, Object> expressTrace(Integer orderId, String role) {
        Map<String, Object> result = new LinkedHashMap<>();
        EbBarterOrder o = baseMapper.selectById(orderId);
        if (o == null) throw new CrmebException("换物单不存在");

        Integer uid = userService.getUserIdException();
        boolean isOwner = o.getOwnerUid() != null && o.getOwnerUid().equals(uid);
        if (!isOwner && (o.getApplyUid() == null || !o.getApplyUid().equals(uid))) {
            throw new CrmebException("无权查看该换物单");
        }
        if (o.getDeliveryType() == null || o.getDeliveryType() != 2) {
            result.put("available", false);
            result.put("tip", "该换物单为自提，没有物流信息");
            return result;
        }

        // 未指定角色时取「我」自己这一侧
        boolean askOwner = "owner".equals(role) || (StrUtil.isBlank(role) && isOwner);
        String no = askOwner ? o.getOwnerLogisticsNo() : o.getApplyLogisticsNo();
        String company = askOwner ? o.getOwnerLogisticsCompany() : o.getApplyLogisticsCompany();
        result.put("side", askOwner ? "owner" : "apply");

        if (StrUtil.isBlank(no)) {
            result.put("available", false);
            result.put("tip", askOwner ? "对方（物主）尚未填写快递单号" : "对方（申请人）尚未填写快递单号");
            return result;
        }

        // 兼容历史数据（库里可能存中文名），统一成物流查询接口认识的编码
        String code = BarterConstants.expressCode(company);
        result.put("number", no);
        result.put("companyCode", code);
        result.put("companyName", BarterConstants.expressName(code));

        // 顺丰类快递查询需要「收/发件人手机号后四位」，从该侧收货地址文本里取
        String addr = askOwner ? o.getOwnerAddress() : o.getApplyAddress();
        String phoneTail = matchPhoneTail(addr);

        try {
            // 物流查询统一走阿里云市场通道（logistics_type=2），鉴权、缓存、降级都由 LogisticService 统一处理。
            // 工程内的 KdniaoExpressClient 保留备用，不再参与调用链。
            LogisticsResultVo vo = logisticService.info(no, null, code, phoneTail);
            if (vo == null || vo.getList() == null || vo.getList().isEmpty()) {
                result.put("available", false);
                result.put("tip", "暂未查询到物流轨迹，请核对单号或稍后再试");
                return result;
            }
            result.put("available", true);
            result.put("deliveryStatus", vo.getDeliveryStatus());
            result.put("isSign", vo.getIsSign());
            result.put("expName", StrUtil.isBlank(vo.getExpName()) ? BarterConstants.expressName(code) : vo.getExpName());
            List<Map<String, Object>> traces = new ArrayList<>();
            for (LogisticsResultListVo t : vo.getList()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("time", t.getTime());
                m.put("status", t.getStatus());
                traces.add(m);
            }
            result.put("list", traces);
            return result;
        } catch (Exception e) {
            String msg = StrUtil.isBlank(e.getMessage()) ? "请检查后台「物流设置」中的查询服务配置" : e.getMessage();
            if (!msg.startsWith("物流查询")) {
                msg = "物流查询失败：" + msg;
            }
            result.put("available", false);
            result.put("tip", msg);
            return result;
        }
    }


    @Override
    public CommonPage<Map<String, Object>> adminRichList(BarterOrderSearchRequest request, PageParamRequest pageParamRequest) {
        CommonPage<EbBarterOrder> page = adminList(request, pageParamRequest);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (page == null || page.getList() == null) return CommonPage.restPage(rows);

        // 批量取昵称与物品标题：N+1 查询在列表页会放大成几十次，这里一次查完
        List<Integer> uids = new ArrayList<>();
        List<Integer> itemIds = new ArrayList<>();
        for (EbBarterOrder o : page.getList()) {
            if (o.getOwnerUid() != null) uids.add(o.getOwnerUid());
            if (o.getApplyUid() != null) uids.add(o.getApplyUid());
            if (o.getItemId() != null) itemIds.add(o.getItemId());
        }
        Map<Integer, User> userMap = new HashMap<>();
        if (!uids.isEmpty()) {
            List<User> users = userService.list(new QueryWrapper<User>().in("uid", uids));
            if (users != null) for (User u : users) userMap.put(u.getUid(), u);
        }
        Map<Integer, EbBarterItem> itemMap = new HashMap<>();
        if (!itemIds.isEmpty()) {
            List<EbBarterItem> items = barterItemDao.selectList(new QueryWrapper<EbBarterItem>().in("id", itemIds));
            if (items != null) for (EbBarterItem it : items) itemMap.put(it.getId(), it);
        }

        for (EbBarterOrder o : page.getList()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("orderSn", o.getOrderSn());
            m.put("status", o.getStatus());
            m.put("statusText", orderStatusText(o.getStatus()));
            m.put("deliveryType", o.getDeliveryType());
            m.put("deliveryTypeText", o.getDeliveryType() != null && o.getDeliveryType() == 1 ? "同城自提" : "快递");
            m.put("ownerUid", o.getOwnerUid());
            m.put("applyUid", o.getApplyUid());
            User owner = userMap.get(o.getOwnerUid());
            User apply = userMap.get(o.getApplyUid());
            m.put("ownerNickname", owner == null ? null : owner.getNickname());
            m.put("ownerAvatar", owner == null ? null : owner.getAvatar());
            m.put("applyNickname", apply == null ? null : apply.getNickname());
            m.put("applyAvatar", apply == null ? null : apply.getAvatar());
            m.put("itemId", o.getItemId());
            EbBarterItem it = itemMap.get(o.getItemId());
            m.put("itemTitle", it == null ? null : it.getTitle());
            m.put("itemCover", it == null ? null : firstImage(it.getImages()));
            m.put("chainNo", o.getChainNo());
            m.put("chainSize", o.getChainSize());
            m.put("createTime", o.getCreateTime());
            m.put("completeTime", o.getCompleteTime());
            rows.add(m);
        }
        CommonPage<Map<String, Object>> result = new CommonPage<>();
        result.setList(rows);
        result.setTotal(page.getTotal());
        result.setPage(page.getPage());
        result.setLimit(page.getLimit());
        result.setTotalPage(page.getTotalPage());
        return result;
    }

    /* ==================== 后台：换物单详情（含聊天记录 / 双方物流 / 互评） ==================== */

    @Override
    public BarterOrderAdminDetailResponse adminDetail(Integer orderId) {
        EbBarterOrder o = baseMapper.selectById(orderId);
        if (o == null) return null;

        BarterOrderAdminDetailResponse r = new BarterOrderAdminDetailResponse();
        r.setId(o.getId());
        r.setOrderSn(o.getOrderSn());
        r.setStatus(o.getStatus());
        r.setStatusText(orderStatusText(o.getStatus()));
        r.setDeliveryType(o.getDeliveryType());
        r.setDeliveryTypeText(o.getDeliveryType() != null && o.getDeliveryType() == 1 ? "自提" : "快递");
        r.setSelfTakeAddress(o.getSelfTakeAddress());
        r.setApplyMessage(o.getApplyMessage());
        r.setItem(toAdminItem(barterItemDao.selectById(o.getItemId())));
        r.setOfferItems(toAdminItems(o.getOfferItemIds()));
        r.setOwner(buildAdminParty(o, true));
        r.setApplyUser(buildAdminParty(o, false));

        r.setChainNo(o.getChainNo());
        r.setChainSeq(o.getChainSeq());
        r.setChainSize(o.getChainSize());
        r.setPathFeePoints(o.getPathFeePoints());
        r.setPathFeeCharged(o.getPathFeeCharged());
        r.setPathFeePaid(o.getPathFeePaid());
        try {
            r.setPathFeeFrozen(o.getPathFeeFrozen());
        } catch (Exception ignore) {
            // 老库可能没有该列，忽略
        }
        r.setCreateTime(o.getCreateTime());
        r.setCompleteTime(o.getCompleteTime());

        // 聊天记录：按时间正序，客服排障时能看到完整交涉过程（含定位/地址类消息）
        List<BarterOrderAdminDetailResponse.Chat> chats = new ArrayList<>();
        try {
            List<EbBarterOrderMessage> msgs = barterOrderMessageService.listByOrder(orderId);
            if (msgs != null) {
                for (EbBarterOrderMessage m : msgs) {
                    BarterOrderAdminDetailResponse.Chat c = new BarterOrderAdminDetailResponse.Chat();
                    c.setId(m.getId());
                    c.setFromUid(m.getFromUid());
                    User from = m.getFromUid() == null ? null : userService.getById(m.getFromUid());
                    c.setFromNickname(from == null ? ("用户" + m.getFromUid()) : from.getNickname());
                    c.setToUid(m.getToUid());
                    c.setType(m.getType());
                    c.setTypeText(chatTypeText(m.getType()));
                    c.setContent(m.getContent());
                    c.setAddress(m.getAddress());
                    c.setCreateTime(m.getCreateTime());
                    chats.add(c);
                }
            }
        } catch (Exception ignore) {
        }
        r.setMessages(chats);

        // 互评：双方对彼此的评价
        List<BarterOrderAdminDetailResponse.Evaluate> evals = new ArrayList<>();
        try {
            List<EbBarterEvaluate> list = barterEvaluateService.lambdaQuery()
                    .eq(EbBarterEvaluate::getOrderId, orderId).list();
            if (list != null) {
                for (EbBarterEvaluate e : list) {
                    BarterOrderAdminDetailResponse.Evaluate ev = new BarterOrderAdminDetailResponse.Evaluate();
                    ev.setId(e.getId());
                    ev.setUid(e.getUid());
                    ev.setToUid(e.getToUid());
                    ev.setStar(e.getStar());
                    ev.setContent(e.getContent());
                    ev.setCreateTime(e.getCreateTime());
                    User u1 = e.getUid() == null ? null : userService.getById(e.getUid());
                    User u2 = e.getToUid() == null ? null : userService.getById(e.getToUid());
                    ev.setNickname(u1 == null ? null : u1.getNickname());
                    ev.setToNickname(u2 == null ? null : u2.getNickname());
                    evals.add(ev);
                }
            }
        } catch (Exception ignore) {
        }
        r.setEvaluates(evals);
        return r;
    }

    /** 后台详情用：物品精简信息（含封面与成色文案） */
    private BarterOrderAdminDetailResponse.Item toAdminItem(EbBarterItem item) {
        if (item == null) return null;
        BarterOrderAdminDetailResponse.Item it = new BarterOrderAdminDetailResponse.Item();
        it.setId(item.getId());
        it.setTitle(item.getTitle());
        it.setImages(item.getImages());
        it.setCover(firstImage(item.getImages()));
        it.setCity(item.getCity());
        it.setConditionLevel(item.getConditionLevel());
        it.setConditionText(conditionText(item.getConditionLevel()));
        it.setStatus(item.getStatus());
        return it;
    }

    private List<BarterOrderAdminDetailResponse.Item> toAdminItems(String ids) {
        List<BarterOrderAdminDetailResponse.Item> list = new ArrayList<>();
        if (StrUtil.isBlank(ids)) return list;
        for (String id : ids.split(",")) {
            if (id.trim().isEmpty()) continue;
            EbBarterItem it = barterItemDao.selectById(id.trim());
            if (it != null) list.add(toAdminItem(it));
        }
        return list;
    }

    /** 后台详情用：参与方（含该侧物流与地址） */
    private BarterOrderAdminDetailResponse.Party buildAdminParty(EbBarterOrder o, boolean owner) {
        Integer uid = owner ? o.getOwnerUid() : o.getApplyUid();
        BarterOrderAdminDetailResponse.Party p = new BarterOrderAdminDetailResponse.Party();
        p.setUid(uid);
        if (uid != null) {
            User u = userService.getById(uid);
            if (u != null) {
                p.setNickname(u.getNickname());
                p.setAvatar(u.getAvatar());
                p.setPhone(u.getPhone());
                p.setCreditScore(u.getCreditScore());
                p.setExchangeCount(u.getBarterExchangeCount());
                p.setLevelId(u.getBarterLevelId());
                if (u.getBarterLevelId() != null) {
                    EbBarterMemberLevel lv = memberLevelService.getById(u.getBarterLevelId());
                    if (lv != null) p.setLevelName(lv.getName());
                }
            }
        }
        if (owner) {
            p.setAddress(o.getOwnerAddress());
            p.setDeliverStatus(o.getOwnerDeliverStatus());
            p.setLogisticsCompany(o.getOwnerLogisticsCompany());
            p.setLogisticsCompanyName(BarterConstants.expressName(o.getOwnerLogisticsCompany()));
            p.setLogisticsNo(o.getOwnerLogisticsNo());
            p.setDeliverTime(o.getOwnerDeliverTime());
            p.setConfirmRecv(o.getOwnerConfirmRecv());
            p.setLogisticsEdit(o.getOwnerLogisticsEdit());
        } else {
            p.setAddress(o.getApplyAddress());
            p.setDeliverStatus(o.getApplyDeliverStatus());
            p.setLogisticsCompany(o.getApplyLogisticsCompany());
            p.setLogisticsCompanyName(BarterConstants.expressName(o.getApplyLogisticsCompany()));
            p.setLogisticsNo(o.getApplyLogisticsNo());
            p.setDeliverTime(o.getApplyDeliverTime());
            p.setConfirmRecv(o.getApplyConfirmRecv());
            p.setLogisticsEdit(o.getApplyLogisticsEdit());
        }
        return p;
    }

    /** 聊天消息类型文案（1 文本 2 图片 3 位置，其余按文本处理） */
    private String chatTypeText(Integer type) {
        if (type == null) return "文本";
        switch (type) {
            case 2: return "图片";
            case 3: return "位置";
            default: return "文本";
        }
    }

    /** 取图片串里的第一张作为封面 */
    private String firstImage(String images) {
        if (StrUtil.isBlank(images)) return null;
        String[] arr = images.split(",");
        for (String s : arr) {
            if (StrUtil.isNotBlank(s)) return s.trim();
        }
        return null;
    }

}
