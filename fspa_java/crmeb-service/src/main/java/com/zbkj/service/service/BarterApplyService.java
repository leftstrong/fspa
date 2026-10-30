package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.barter.EbBarterApply;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.barter.BarterApplyDetailResponse;

/**
 * 换物申请/提案 业务接口
 */
public interface BarterApplyService extends IService<EbBarterApply> {

    /** 发起换物申请（applyUid 从登录态取） */
    Integer createApply(EbBarterApply apply);

    /**
     * 主人接受 -> 生成换物单
     *
     * @param applyId         申请ID
     * @param deliveryType    交付方式：1自提 2快递
     * @param selfTakeAddress 自提地址（自提时必填）
     */
    Boolean accept(Integer applyId, Integer deliveryType, String selfTakeAddress);

    /** 主人拒绝 */
    Boolean reject(Integer applyId);

    /** 申请人取消 */
    Boolean cancel(Integer applyId);

    /** 我收到的申请（owner 视角），status 为空查全部，否则按申请状态过滤 */
    CommonPage<EbBarterApply> incomingList(PageParamRequest pageParamRequest, Integer status);

    /** 我发起的申请（apply 视角），status 为空查全部，否则按申请状态过滤 */
    CommonPage<EbBarterApply> myApplyList(PageParamRequest pageParamRequest, Integer status);

    /**
     * 申请详情（消息直达二级页用）：带出双方物品/用户/操作权限，非双方返回 null
     */
    BarterApplyDetailResponse detail(Integer applyId);
}
