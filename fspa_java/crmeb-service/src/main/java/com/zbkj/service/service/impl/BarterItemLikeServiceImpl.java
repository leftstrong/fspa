package com.zbkj.service.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.constants.BarterConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.barter.EbBarterItem;
import com.zbkj.common.model.barter.EbBarterItemLike;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.barter.BarterItemResponse;
import com.zbkj.service.dao.BarterItemDao;
import com.zbkj.service.dao.BarterItemLikeDao;
import com.zbkj.service.service.BarterItemLikeService;
import com.zbkj.service.service.BarterItemService;
import com.zbkj.service.service.BarterMessageService;
import com.zbkj.service.service.UserService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 「我想要」接口实现类
 */
@Service("barterItemLikeService")
public class BarterItemLikeServiceImpl extends ServiceImpl<BarterItemLikeDao, EbBarterItemLike> implements BarterItemLikeService {

    @Autowired
    private BarterItemDao barterItemDao;
    @Autowired
    private BarterItemService barterItemService;
    @Autowired
    private UserService userService;
    @Autowired
    private BarterMessageService barterMessageService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public Boolean toggle(Integer itemId, Integer uid) {
        if (itemId == null || uid == null) throw new CrmebException("参数不完整");
        EbBarterItem item = barterItemDao.selectById(itemId);
        if (item == null) throw new CrmebException("物品不存在");
        if (uid.equals(item.getUid())) throw new CrmebException("不能想要自己发布的物品");
        // 已下架/已交换的物品不再允许新点赞，避免误导
        if (item.getStatus() != null && item.getStatus() != 1) {
            throw new CrmebException("该物品当前不可交换");
        }
        EbBarterItemLike exist = baseMapper.selectOne(new QueryWrapper<EbBarterItemLike>()
                .eq("item_id", itemId).eq("uid", uid).last("LIMIT 1"));
        boolean want = exist == null;
        transactionTemplate.execute(e -> {
            if (want) {
                EbBarterItemLike rec = new EbBarterItemLike();
                rec.setItemId(itemId);
                rec.setUid(uid);
                rec.setCreateTime(DateUtil.date());
                baseMapper.insert(rec);
                barterItemDao.incrWantCount(itemId, 1);
            } else {
                baseMapper.deleteById(exist.getId());
                barterItemDao.incrWantCount(itemId, -1);
            }
            return Boolean.TRUE;
        });
        // 站内消息：仅「新增想要」时通知物品主人（取消不发，避免骚扰）
        if (want) {
            User liker = userService.getById(uid);
            String nick = liker != null && liker.getNickname() != null ? liker.getNickname() : "有人";
            barterMessageService.push(item.getUid(), BarterConstants.MSG_ITEM_WANTED, itemId,
                    "有人想要你的好物", nick + " 想要你的《" + item.getTitle() + "》，去看看要不要发起换物");
        }
        return want;
    }

    @Override
    public Boolean isWant(Integer itemId, Integer uid) {
        if (itemId == null || uid == null) return Boolean.FALSE;
        return baseMapper.selectCount(new QueryWrapper<EbBarterItemLike>()
                .eq("item_id", itemId).eq("uid", uid)) > 0;
    }

    @Override
    public List<Integer> wantedItemIds(Integer uid, List<Integer> itemIds) {
        if (uid == null || itemIds == null || itemIds.isEmpty()) return Collections.emptyList();
        return baseMapper.selectList(new QueryWrapper<EbBarterItemLike>()
                        .eq("uid", uid).in("item_id", itemIds))
                .stream().map(EbBarterItemLike::getItemId).distinct().collect(Collectors.toList());
    }

    @Override
    public CommonPage<BarterItemResponse> myWantList(PageParamRequest pageParamRequest) {
        Integer uid = userService.getUserIdException();
        Page<Object> startPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        List<EbBarterItemLike> likes = baseMapper.selectList(new QueryWrapper<EbBarterItemLike>()
                .eq("uid", uid).orderByDesc("id"));
        List<BarterItemResponse> resp = Collections.emptyList();
        if (!likes.isEmpty()) {
            List<Integer> itemIds = likes.stream().map(EbBarterItemLike::getItemId).distinct().collect(Collectors.toList());
            List<EbBarterItem> items = barterItemDao.selectBatchIds(itemIds);
            // 保持「最近想要」的顺序
            items.sort((a, b) -> itemIds.indexOf(b.getId()) - itemIds.indexOf(a.getId()));
            resp = barterItemService.buildResponses(items);
        }
        return CommonPage.restPage(CommonPage.copyPageInfo(startPage, resp));
    }
}
