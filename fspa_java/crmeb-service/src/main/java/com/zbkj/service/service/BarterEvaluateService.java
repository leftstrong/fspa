package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.barter.EbBarterEvaluate;

import java.util.List;

/**
 * 换物互评 业务接口
 */
public interface BarterEvaluateService extends IService<EbBarterEvaluate> {

    /** 提交评价（uid 从登录态取） */
    Boolean addEvaluate(EbBarterEvaluate evaluate);

    /** 某个用户收到的全部评价（信用展示用） */
    List<EbBarterEvaluate> listByToUid(Integer toUid);
}
