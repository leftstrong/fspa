package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.barter.EbBarterPointsLog;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.page.CommonPage;

/**
 * 换物积分日志 业务接口
 */
public interface BarterPointsLogService extends IService<EbBarterPointsLog> {

    /**
     * 记录积分变动并保持用户积分余额一致，返回最新余额
     * @param type    1获取 2消费
     * @param linkType 1置顶 2完成交换奖励 3发布奖励 4邀请
     */
    Integer changePoints(Integer uid, Integer type, Integer value, String mark, Integer linkType, Integer linkId);

    /** 我的积分明细（按时间倒序） */
    CommonPage<EbBarterPointsLog> myLog(Integer uid, PageParamRequest pageRequest);

    /**
     * 冻结积分：可用余额 −amount、冻结 +amount（发起智能换物时调用，防止成交前把分花光）
     *
     * @return 冻结后的可用余额（null = 用户不存在或金额非法）
     */
    Integer freezePoints(Integer uid, Integer amount, String mark, Integer linkId);

    /**
     * 解冻积分：可用余额 +amount、冻结 −amount（未成交/取消时调用）
     *
     * @return 解冻后的可用余额（null = 用户不存在或金额非法）
     */
    Integer unfreezePoints(Integer uid, Integer amount, String mark, Integer linkId);

    /**
     * 冻结转实扣：只减冻结额，不再动可用余额（成交时调用，发起时已扣过）
     *
     * @return 最新可用余额（null = 用户不存在或金额非法）
     */
    Integer settleFrozenPoints(Integer uid, Integer amount, String mark, Integer linkId);
}
