package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.barter.EbBarterItem;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.BarterItemSearchRequest;
import com.zbkj.common.response.barter.BarterItemResponse;

import java.util.List;

/**
 * 闲置物品 业务接口
 */
public interface BarterItemService extends IService<EbBarterItem> {

    /** 发布/编辑好物（uid 从登录态取，禁止客户端传） */
    Boolean publish(EbBarterItem item);

    /** 前台信息流：在售 + 置顶优先 + 等级曝光权重 + 最新 */
    CommonPage<BarterItemResponse> frontList(BarterItemSearchRequest request, PageParamRequest pageParamRequest);

    /** 我的发布（status 为空查全部，否则按物品状态过滤） */
    CommonPage<BarterItemResponse> myList(PageParamRequest pageParamRequest, Integer status);

    /** 详情（含发布者信用/等级） */
    BarterItemResponse detail(Integer id);

    /** 消耗积分置顶，时长/单价后台可配 */
    Boolean topItem(Integer id, Integer hours);

    /** 置顶单价（积分/小时），后台 system_config: barter_top_points_per_hour */
    int topPointsPerHour();

    /** 置顶默认时长（小时），后台 system_config: barter_top_duration_hour */
    int topDurationHour();

    /** 管理后台分页（含审核） */
    CommonPage<BarterItemResponse> adminList(BarterItemSearchRequest request, PageParamRequest pageParamRequest);

    /** 审核：通过(status=1)/驳回(status=3)/下架(status=0) */
    Boolean audit(Integer id, Integer status);

    /** 审核（带驳回原因）：驳回(status=3)时写入 rejectReason，便于审核记录追溯 */
    Boolean audit(Integer id, Integer status, String reason);

    /** 后台直接调整物品状态（纠纷结案等平台手动干预，不推送审核结果消息） */
    Boolean updateStatus(Integer id, Integer status);

    /** 编辑好物（物品主人，保存后重新进入审核 status=2） */
    Boolean edit(EbBarterItem item);

    /** 重新发起审核（已驳回的好物，不改内容直接置为审核中 status=2） */
    Boolean reApply(Integer id);

    /** 下架好物（仅在售态可下架，置为已下架 status=6，下架后需重新发布） */
    Boolean offshelf(Integer id);

    /**
     * 继续换下一件：把「换物接力收到的」已交换物品重新上架为在售。
     * 只允许 receive_from_order_id 非空的物品（即确实通过换物收到的），
     * 避免把自己已经换出去的旧物品重新挂出来。
     */
    Boolean relist(Integer id);

    /**
     * 把物品列表组装为响应对象（附加发布者昵称/等级/信用分、置顶标记、「我想要」数与当前用户是否想要）
     * 供信息流/详情/我的想要等场景复用
     */
    List<BarterItemResponse> buildResponses(List<EbBarterItem> items);

    /** 在售物品的城市列表（去重，好物页城市筛选用） */
    List<String> onSaleCities();
}
