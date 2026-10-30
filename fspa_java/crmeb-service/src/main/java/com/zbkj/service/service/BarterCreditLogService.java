package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.barter.EbBarterCreditLog;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.page.CommonPage;

/**
 * 换物信用分日志 业务接口
 */
public interface BarterCreditLogService extends IService<EbBarterCreditLog> {

    /**
     * 记录信用分变动（delta 可正可负）并同步用户信用分余额，返回最新信用分
     *
     * @param uid      用户
     * @param delta    变动值（正=加分，负=扣分）
     * @param mark     说明
     * @param linkType 1 换物完成 2 互评 3 平台调整
     */
    Integer changeCredit(Integer uid, Integer delta, String mark, Integer linkType, Integer linkId);

    /** 我的信用分明细（按时间倒序） */
    CommonPage<EbBarterCreditLog> myLog(Integer uid, PageParamRequest pageRequest);
}
