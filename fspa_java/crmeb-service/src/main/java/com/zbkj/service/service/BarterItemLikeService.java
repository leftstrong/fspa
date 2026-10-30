package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.barter.EbBarterItemLike;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.barter.BarterItemResponse;

import java.util.List;

/**
 * 「我想要」业务接口
 * 轻点赞：表达换取意愿；数量参与好物列表「最热」排序；
 * 个人主页「我的想要」列出曾点赞物品（含已下架/已交换状态）。
 */
public interface BarterItemLikeService extends IService<EbBarterItemLike> {

    /**
     * 切换「我想要」状态（点赞/取消）
     *
     * @return true=已想要 false=已取消
     */
    Boolean toggle(Integer itemId, Integer uid);

    /** 当前用户是否想要过该物品 */
    Boolean isWant(Integer itemId, Integer uid);

    /** 批量：当前用户想要过的物品ID集合（列表页回显 isWant 用，避免 N 次查询） */
    List<Integer> wantedItemIds(Integer uid, List<Integer> itemIds);

    /** 我的想要列表（不按物品 status 过滤，需回显下架/已交换） */
    CommonPage<BarterItemResponse> myWantList(PageParamRequest pageParamRequest);
}
