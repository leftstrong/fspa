package com.zbkj.service.service;

import java.util.Map;

/**
 * 以物换物 通知服务（换物关键节点）
 * 仅做小程序订阅消息推送，失败不影响主流程
 */
public interface BarterNotifyService {

    /** 收到新的换物申请：通知物品主人 */
    void notifyApplyCreated(Integer ownerUid, String itemTitle, String applicantName);

    /** 申请被接受：通知申请人 */
    void notifyApplyAccepted(Integer applyUid, String itemTitle);

    /** 一方发货：通知另一方 */
    void notifyDelivered(Integer targetUid, String itemTitle, String deliverRole);

    /** 换物完成：通知双方 */
    void notifyCompleted(Integer uid, String itemTitle);

    /** 申请/换物单取消或关闭：通知对端 */
    void notifyCancelled(Integer uid, String itemTitle, String reason);
}
