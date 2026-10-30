package com.zbkj.service.service;

import com.zbkj.common.model.barter.EbBarterOrderMessage;

import java.util.List;

public interface BarterOrderMessageService {
    /** 发送一条单内消息（同时给接收方推送红点通知） */
    EbBarterOrderMessage send(Integer orderId, Integer fromUid, Integer toUid,
                              Integer type, String content, String lat, String lng, String address);

    /** 按 orderId 拉取双向消息（按时间正序，前端直接渲染） */
    List<EbBarterOrderMessage> listByOrder(Integer orderId);
}
