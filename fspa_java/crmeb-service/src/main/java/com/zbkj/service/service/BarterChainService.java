package com.zbkj.service.service;

import com.zbkj.common.request.BarterChainCreateRequest;
import com.zbkj.common.request.BarterChainSearchRequest;
import com.zbkj.common.response.barter.BarterChainCreateResponse;
import com.zbkj.common.response.barter.BarterChainSearchResponse;

/**
 * 智能换物（多方交换环）业务接口
 *
 * 模型说明：交换环是「每个人给出自己的一件，得到自己期望的一件」的闭环。
 * 物品流向 A → X1 → … → B → A，环上人数 = 跳数 + 1；
 * 用户指定的 B 必然在环上（否则用户拿不到想要的东西），A 最终收到 B。
 *
 * 计费：检索免费；只有确认成环下单才扣积分，档位 1/2/3 跳 = 10/20/50。
 */
public interface BarterChainService {

    /**
     * 检索交换环（免费）
     * @param request myItemId=我的在售物 A，targetItemId=想要的在售物 B，maxHops=最大跨越次数
     * @return 候选环（按跳数升序，最多 5 条）与不可行原因
     */
    BarterChainSearchResponse search(BarterChainSearchRequest request);

    /**
     * 按选定环成环下单：校验 → 扣积分 → 生成 N 张 1v1 换物单 → 锁定物品 → 通知参与者
     * @return 环号、实扣积分、订单 id 列表
     */
    BarterChainCreateResponse createChain(BarterChainCreateRequest request);

    /**
     * 环详情（仅环上参与者可见）：按链表顺序展开每张 1v1 订单的状态
     * @param chainNo 环号
     */
    BarterChainCreateResponse.Detail detail(String chainNo);
}
