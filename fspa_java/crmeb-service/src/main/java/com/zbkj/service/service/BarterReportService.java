package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.barter.EbBarterReport;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.barter.BarterReportAdminDetailResponse;
import com.zbkj.common.response.barter.BarterReportAdminResponse;

import java.util.Map;

/**
 * 换物举报 业务接口
 */
public interface BarterReportService extends IService<EbBarterReport> {

    /** 提交举报（uid 从登录态取） */
    Boolean createReport(EbBarterReport report);

    /** 平台处理：通过(status=1)/驳回(status=2) + 结果说明 */
    Boolean handle(Integer id, Integer status, String result, Integer handleUid);

    /**
     * 各状态举报数量，供后台 TAB 红点展示。
     * 返回键：all / pending(0) / handled(1) / rejected(2)
     */
    Map<String, Integer> adminStatusCounts();

    /**
     * 后台举报列表（富化）。
     * 补物品标题与封面、举报人与被举报人昵称头像，避免运营对着 uid 反查。
     *
     * @param status 0待处理 1已处理 2已驳回，null 查全部
     */
    CommonPage<BarterReportAdminResponse> adminRichList(Integer status, PageParamRequest pageParamRequest);

    /**
     * 后台举报明细：物品、关联换物单、双方档案、同一被举报人的其他待处理举报。
     *
     * @return null 表示举报不存在
     */
    BarterReportAdminDetailResponse adminDetail(Integer id);
}
