package com.zbkj.service.service.impl;

import com.zbkj.common.constants.BarterConstants;
import com.zbkj.common.model.barter.EbBarterOrderMessage;
import com.zbkj.service.dao.BarterOrderMessageDao;
import com.zbkj.service.service.BarterMessageService;
import com.zbkj.service.service.BarterOrderMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service("barterOrderMessageService")
public class BarterOrderMessageServiceImpl implements BarterOrderMessageService {

    @Autowired
    private BarterOrderMessageDao messageDao;
    @Autowired
    private BarterMessageService barterMessageService;

    @Override
    public EbBarterOrderMessage send(Integer orderId, Integer fromUid, Integer toUid,
                                     Integer type, String content, String lat, String lng, String address) {
        EbBarterOrderMessage m = new EbBarterOrderMessage();
        m.setOrderId(orderId);
        m.setFromUid(fromUid);
        m.setToUid(toUid);
        m.setType(type == null ? 1 : type);
        m.setContent(content);
        m.setLat(lat);
        m.setLng(lng);
        m.setAddress(address);
        m.setCreateTime(new Date());
        messageDao.insert(m);
        // 给接收方推一个「换物单」分组红点，避免对方不知道有人发消息
        String preview = type != null && type == 2 ? "[图片]" : (type != null && type == 3 ? "[位置]" : (content == null ? "" : content));
        barterMessageService.push(toUid, BarterConstants.MSG_ORDER_CHAT, orderId, "换物单新消息", preview);
        return m;
    }

    @Override
    public List<EbBarterOrderMessage> listByOrder(Integer orderId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EbBarterOrderMessage> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        qw.eq("order_id", orderId);
        qw.orderByAsc("id");
        return messageDao.selectList(qw);
    }
}
