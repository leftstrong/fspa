package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.barter.EbBarterCategory;

import java.util.List;

/**
 * 换物品类 业务接口
 */
public interface BarterCategoryService extends IService<EbBarterCategory> {

    /**
     * 前台品类列表（仅启用项，按 sort、id 升序）
     */
    List<EbBarterCategory> frontList();
}
