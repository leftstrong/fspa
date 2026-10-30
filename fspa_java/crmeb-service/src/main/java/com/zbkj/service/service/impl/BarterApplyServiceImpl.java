package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.constants.BarterConstants;
import com.zbkj.common.model.barter.EbBarterApply;
import com.zbkj.common.model.barter.EbBarterItem;
import com.zbkj.common.model.barter.EbBarterOrder;
import com.zbkj.common.model.barter.EbBarterUserAddress;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.barter.BarterApplyDetailResponse;
import com.zbkj.service.dao.BarterApplyDao;
import com.zbkj.service.dao.BarterItemDao;
import com.zbkj.service.dao.BarterOrderDao;
import com.zbkj.service.service.BarterApplyService;
import com.zbkj.service.service.BarterMessageService;
import com.zbkj.service.service.BarterNotifyService;
import com.zbkj.service.service.BarterUserAddressService;
import com.zbkj.service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 换物申请/提案 接口实现类
 */
@Service("barterApplyService")
public class BarterApplyServiceImpl extends ServiceImpl<BarterApplyDao, EbBarterApply> implements BarterApplyService {

    @Autowired
    private UserService userService;
    @Autowired
    private BarterItemDao itemDao;
    @Autowired
    private BarterOrderDao orderDao;
    @Autowired
    private BarterUserAddressService barterUserAddressService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private BarterNotifyService barterNotifyService;
    @Autowired
    private BarterMessageService barterMessageService;

    @Override
    public Integer createApply(EbBarterApply apply) {
        Integer uid = userService.getUserIdException();
        EbBarterItem item = itemDao.selectById(apply.getItemId());
        if (item == null || item.getStatus() != 1) return -1; // 物品不存在或未在售
        if (item.getUid().equals(uid)) return -1; // 不能和自己换
        apply.setApplyUid(uid);
        apply.setOwnerUid(item.getUid());
        apply.setStatus(1); // 待确认
        apply.setCreateTime(new Date());
        apply.setUpdateTime(new Date());
        baseMapper.insert(apply);
        // 站内消息：通知物品主人收到新换物申请
        User applicant = userService.getById(uid);
        String applicantName = applicant != null ? applicant.getNickname() : "有人";
        barterMessageService.push(item.getUid(), BarterConstants.MSG_APPLY_CREATED, apply.getId(),
                "收到新的换物申请", applicantName + " 想用 TA 的物品换你的《" + item.getTitle() + "》");
        // 微信订阅消息（未配置模板/未授权则静默跳过）
        barterNotifyService.notifyApplyCreated(item.getUid(), item.getTitle(), applicantName);
        return apply.getId();
    }

    @Override
    public Boolean accept(Integer applyId, Integer deliveryType, String selfTakeAddress) {
        EbBarterApply apply = baseMapper.selectById(applyId);
        if (apply == null || apply.getStatus() != 1) return false;
        Integer uid = userService.getUserIdException();
        if (!apply.getOwnerUid().equals(uid)) return false; // 仅物品主人可接受
        // 交付方式：1自提 2快递；自提必须填自提点
        int dtype = (deliveryType == null || deliveryType != 2) ? 1 : 2;
        if (dtype == 1 && (selfTakeAddress == null || selfTakeAddress.trim().isEmpty())) return false;
        // 快递：取双方默认收货地址快照（任一缺失则不允许走快递）
        String ownerAddr = null;
        String applyAddr = null;
        if (dtype == 2) {
            ownerAddr = defaultAddressText(apply.getOwnerUid());
            applyAddr = defaultAddressText(apply.getApplyUid());
            if (ownerAddr == null || applyAddr == null) return false;
        }
        final int fDtype = dtype;
        final String fOwnerAddr = ownerAddr;
        final String fApplyAddr = applyAddr;
        final String fSelfTake = dtype == 1 ? selfTakeAddress.trim() : "";
        return transactionTemplate.execute(e -> {
            apply.setStatus(2); // 已接受
            apply.setUpdateTime(new Date());
            baseMapper.updateById(apply);
            // 生成换物单
            EbBarterOrder order = new EbBarterOrder();
            order.setOrderSn(System.currentTimeMillis() + "" + apply.getOwnerUid());
            order.setApplyId(apply.getId());
            order.setItemId(apply.getItemId());
            order.setOwnerUid(apply.getOwnerUid());
            order.setApplyUid(apply.getApplyUid());
            order.setOfferItemIds(apply.getOfferItemIds());
            order.setStatus(1); // 待交付
            order.setDeliveryType(fDtype); // 1自提 2快递
            order.setSelfTakeAddress(fSelfTake);
            order.setOwnerAddress(fOwnerAddr);
            order.setApplyAddress(fApplyAddr);
            order.setOwnerDeliverStatus(0);
            order.setApplyDeliverStatus(0);
            order.setOwnerConfirmRecv(0);
            order.setApplyConfirmRecv(0);
            order.setCreateTime(new Date());
            order.setUpdateTime(new Date());
            orderDao.insert(order);
            apply.setStatus(4); // 已生成换物单
            baseMapper.updateById(apply);
            // 站内消息：申请人「申请被接受」+ 双方「换物单已生成」
            EbBarterItem item2 = itemDao.selectById(apply.getItemId());
            String title2 = item2 != null ? item2.getTitle() : "-";
            barterMessageService.push(apply.getApplyUid(), BarterConstants.MSG_APPLY_ACCEPTED, apply.getId(),
                    "换物申请已被接受", "对方接受了你对《" + title2 + "》的换物申请，请尽快交付");
            Integer oid = order.getId();
            barterMessageService.push(apply.getOwnerUid(), BarterConstants.MSG_ORDER_CREATED, oid,
                    "换物单已生成", "《" + title2 + "》的换物单已生成，" + (fDtype == 2 ? "请使用快递发货并填写物流单号" : "请按约定自提点完成交付"));
            barterMessageService.push(apply.getApplyUid(), BarterConstants.MSG_ORDER_CREATED, oid,
                    "换物单已生成", "《" + title2 + "》的换物单已生成，" + (fDtype == 2 ? "请使用快递发货并填写物流单号" : "请按约定自提点完成交付"));
            // 通知申请人：申请已被接受
            EbBarterItem item = itemDao.selectById(apply.getItemId());
            barterNotifyService.notifyApplyAccepted(apply.getApplyUid(), item != null ? item.getTitle() : "-");
            return Boolean.TRUE;
        });
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

    @Override
    public Boolean reject(Integer applyId) {
        EbBarterApply apply = baseMapper.selectById(applyId);
        if (apply == null || apply.getStatus() != 1) return false;
        Integer uid = userService.getUserIdException();
        if (!apply.getOwnerUid().equals(uid)) return false;
        apply.setStatus(3); // 已拒绝
        apply.setUpdateTime(new Date());
        EbBarterItem item = itemDao.selectById(apply.getItemId());
        String t3 = item != null ? item.getTitle() : "-";
        barterMessageService.push(apply.getApplyUid(), BarterConstants.MSG_APPLY_REJECTED, apply.getId(),
                "换物申请被拒绝", "很遗憾，对方拒绝了你对《" + t3 + "》的换物申请");
        barterNotifyService.notifyCancelled(apply.getApplyUid(), t3, "对方拒绝了换物申请");
        return transactionTemplate.execute(e -> {
            baseMapper.updateById(apply);
            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean cancel(Integer applyId) {
        EbBarterApply apply = baseMapper.selectById(applyId);
        if (apply == null || apply.getStatus() != 1) return false;
        Integer uid = userService.getUserIdException();
        if (!apply.getApplyUid().equals(uid)) return false; // 仅申请人可取消
        apply.setStatus(0); // 已取消
        apply.setUpdateTime(new Date());
        EbBarterItem item = itemDao.selectById(apply.getItemId());
        String t4 = item != null ? item.getTitle() : "-";
        barterMessageService.push(apply.getOwnerUid(), BarterConstants.MSG_APPLY_CANCELLED, apply.getId(),
                "换物申请已取消", "对方取消了对《" + t4 + "》的换物申请");
        barterNotifyService.notifyCancelled(apply.getOwnerUid(), t4, "对方取消了换物申请");
        return transactionTemplate.execute(e -> {
            baseMapper.updateById(apply);
            return Boolean.TRUE;
        });
    }

    @Override
    public BarterApplyDetailResponse detail(Integer applyId) {
        EbBarterApply apply = baseMapper.selectById(applyId);
        if (apply == null) return null;
        Integer uid = userService.getUserIdException();
        // 仅申请双方可见
        if (!apply.getOwnerUid().equals(uid) && !apply.getApplyUid().equals(uid)) return null;

        BarterApplyDetailResponse vo = new BarterApplyDetailResponse();
        vo.setId(apply.getId());
        vo.setItemId(apply.getItemId());
        vo.setApplyUid(apply.getApplyUid());
        vo.setOwnerUid(apply.getOwnerUid());
        vo.setMessage(apply.getMessage());
        vo.setStatus(apply.getStatus());
        vo.setStatusText(applyStatusText(apply.getStatus()));
        vo.setCreateTime(apply.getCreateTime());
        vo.setItem(toSimpleItem(itemDao.selectById(apply.getItemId())));
        vo.setOfferItems(toSimpleItems(apply.getOfferItemIds()));
        vo.setApplyUser(toSimpleUser(apply.getApplyUid()));
        vo.setOwnerUser(toSimpleUser(apply.getOwnerUid()));

        boolean isOwner = apply.getOwnerUid().equals(uid);
        vo.setMyRole(isOwner ? "owner" : "apply");
        // 待确认状态下：物主可接受/拒绝，申请人可取消
        vo.setCanAccept(isOwner && apply.getStatus() == 1);
        vo.setCanCancel(!isOwner && apply.getStatus() == 1);

        // 已接受则会生成换物单，带出供前端跳转
        QueryWrapper<EbBarterOrder> qw = new QueryWrapper<>();
        qw.eq("apply_id", apply.getId());
        qw.orderByDesc("id");
        List<EbBarterOrder> orders = orderDao.selectList(qw);
        if (orders != null && !orders.isEmpty()) vo.setOrderId(orders.get(0).getId());
        return vo;
    }

    private String applyStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "已取消";
            case 1: return "待确认";
            case 2: return "已接受";
            case 3: return "已拒绝";
            case 4: return "已生成换物单";
            default: return "未知";
        }
    }

    private BarterApplyDetailResponse.SimpleItem toSimpleItem(EbBarterItem item) {
        if (item == null) return null;
        BarterApplyDetailResponse.SimpleItem s = new BarterApplyDetailResponse.SimpleItem();
        s.setId(item.getId());
        s.setTitle(item.getTitle());
        s.setImages(item.getImages());
        s.setCover(item.getImages() == null ? "" : item.getImages().split(",")[0]);
        s.setCity(item.getCity());
        s.setConditionLevel(item.getConditionLevel());
        s.setConditionText(conditionText(item.getConditionLevel()));
        return s;
    }

    private List<BarterApplyDetailResponse.SimpleItem> toSimpleItems(String ids) {
        List<BarterApplyDetailResponse.SimpleItem> list = new ArrayList<>();
        if (ids == null || ids.trim().isEmpty()) return list;
        for (String id : ids.split(",")) {
            if (id.trim().isEmpty()) continue;
            EbBarterItem it = itemDao.selectById(id.trim());
            if (it != null) list.add(toSimpleItem(it));
        }
        return list;
    }

    private BarterApplyDetailResponse.SimpleUser toSimpleUser(Integer uid) {
        if (uid == null) return null;
        BarterApplyDetailResponse.SimpleUser u = new BarterApplyDetailResponse.SimpleUser();
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
    public CommonPage<EbBarterApply> incomingList(PageParamRequest pageParamRequest, Integer status) {
        Integer uid = userService.getUserIdException();
        Page<Object> startPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        QueryWrapper<EbBarterApply> qw = new QueryWrapper<>();
        qw.eq("owner_uid", uid);
        if (status != null) qw.eq("status", status);
        qw.orderByDesc("id");
        List<EbBarterApply> list = baseMapper.selectList(qw);
        fillApplyExtra(list);
        return CommonPage.restPage(CommonPage.copyPageInfo(startPage, list));
    }

    /** 列表补出物品标题/封面与申请人信息，避免前端只能显示 itemId */
    private void fillApplyExtra(List<EbBarterApply> list) {
        if (list == null || list.isEmpty()) return;
        for (EbBarterApply a : list) {
            EbBarterItem item = itemDao.selectById(a.getItemId());
            if (item != null) {
                a.setItemTitle(item.getTitle());
                a.setItemCover(item.getImages() == null ? "" : item.getImages().split(",")[0]);
            }
            User u = userService.getById(a.getApplyUid());
            if (u != null) {
                a.setApplyNickname(u.getNickname());
                a.setApplyAvatar(u.getAvatar());
            }
        }
    }

    @Override
    public CommonPage<EbBarterApply> myApplyList(PageParamRequest pageParamRequest, Integer status) {
        Integer uid = userService.getUserIdException();
        Page<Object> startPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        QueryWrapper<EbBarterApply> qw = new QueryWrapper<>();
        qw.eq("apply_uid", uid);
        if (status != null) qw.eq("status", status);
        qw.orderByDesc("id");
        List<EbBarterApply> list = baseMapper.selectList(qw);
        fillApplyExtra(list);
        return CommonPage.restPage(CommonPage.copyPageInfo(startPage, list));
    }
}
