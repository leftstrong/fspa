package com.zbkj.service.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.constants.BarterConstants;
import com.zbkj.common.model.barter.EbBarterMessage;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.barter.BarterUnreadResponse;
import com.zbkj.service.dao.BarterMessageDao;
import com.zbkj.service.service.BarterMessageService;
import com.zbkj.service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 换物站内消息 实现
 */
@Service("barterMessageService")
public class BarterMessageServiceImpl extends com.baomidou.mybatisplus.extension.service.impl.ServiceImpl<BarterMessageDao, EbBarterMessage> implements BarterMessageService {

    @Autowired
    private BarterMessageDao messageDao;
    @Autowired
    private UserService userService;

    @Override
    public void push(Integer uid, Integer type, Integer bizId, String title, String content) {
        if (uid == null || type == null) return;
        try {
            EbBarterMessage m = new EbBarterMessage();
            m.setUid(uid);
            m.setType(type);
            m.setGroupType(BarterConstants.groupOfMsgType(type));
            m.setBizId(bizId);
            m.setTitle(title);
            m.setContent(content);
            m.setIsRead(0);
            m.setCreateTime(new Date());
            messageDao.insert(m);
        } catch (Exception e) {
            // 消息写失败绝不阻断主业务
            e.printStackTrace();
        }
    }

    @Override
    public CommonPage<EbBarterMessage> myList(String group, PageParamRequest pageRequest) {
        Integer uid = userService.getUserIdException();
        Page<Object> startPage = PageHelper.startPage(pageRequest.getPage(), pageRequest.getLimit());
        QueryWrapper<EbBarterMessage> qw = new QueryWrapper<>();
        qw.eq("uid", uid);
        if (StrUtil.isNotBlank(group)) qw.eq("group_type", group);
        qw.orderByDesc("id");
        return CommonPage.restPage(CommonPage.copyPageInfo(startPage, messageDao.selectList(qw)));
    }

    @Override
    public BarterUnreadResponse unreadSummary() {
        Integer uid = userService.getUserId();
        BarterUnreadResponse r = new BarterUnreadResponse();
        if (uid == null || uid <= 0) return r;
        List<Map<String, Object>> rows = messageDao.countUnreadGroup(uid);
        int total = 0;
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object g = row.get("group_type");
                Object c = row.get("COUNT(1)");
                if (g == null || c == null) continue;
                int n = Integer.parseInt(String.valueOf(c));
                total += n;
                String gs = String.valueOf(g);
                if ("incoming".equals(gs)) r.setIncoming(n);
                else if ("outgoing".equals(gs)) r.setOutgoing(n);
                else if ("order".equals(gs)) r.setOrder(n);
                // want 已并入 system（含历史数据迁移前的残留），统一计入系统
                else r.setSystem(r.getSystem() + n);
            }
        }
        r.setTotal(total);
        return r;
    }

    @Override
    public Boolean markRead(Integer id) {
        Integer uid = userService.getUserIdException();
        EbBarterMessage m = messageDao.selectById(id);
        if (m == null || !uid.equals(m.getUid())) return false;
        m.setIsRead(1);
        m.setReadTime(new Date());
        return messageDao.updateById(m) > 0;
    }

    @Override
    public Boolean markGroupRead(String group) {
        Integer uid = userService.getUserIdException();
        int n = StrUtil.isBlank(group) ? messageDao.markAllRead(uid) : messageDao.markGroupRead(uid, group);
        return n >= 0;
    }
}
