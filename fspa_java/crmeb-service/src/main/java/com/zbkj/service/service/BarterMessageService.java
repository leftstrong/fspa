package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.barter.EbBarterMessage;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.barter.BarterUnreadResponse;

/**
 * 换物站内消息 业务接口
 */
public interface BarterMessageService extends IService<EbBarterMessage> {

    /**
     * 写一条站内消息（同时保留微信订阅推送由 BarterNotifyService 负责）
     *
     * @param uid      接收者
     * @param type     消息类型 BarterConstants.MSG_*
     * @param bizId    关联业务ID
     * @param title    标题
     * @param content  正文
     */
    void push(Integer uid, Integer type, Integer bizId, String title, String content);

    /** 我的消息列表（group 为空查全部） */
    CommonPage<EbBarterMessage> myList(String group, PageParamRequest pageRequest);

    /** 未读汇总（各业务条目红点数字） */
    BarterUnreadResponse unreadSummary();

    /** 标记单条已读 */
    Boolean markRead(Integer id);

    /** 标记整组已读（group 为空则全部已读） */
    Boolean markGroupRead(String group);
}
