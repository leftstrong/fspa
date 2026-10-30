package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.barter.EbBarterCategory;
import com.zbkj.service.dao.BarterCategoryDao;
import com.zbkj.service.service.BarterCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 换物品类 接口实现类
 */
@Service("barterCategoryService")
public class BarterCategoryServiceImpl extends ServiceImpl<BarterCategoryDao, EbBarterCategory> implements BarterCategoryService {

    @Override
    public List<EbBarterCategory> frontList() {
        LambdaQueryWrapper<EbBarterCategory> qw = new LambdaQueryWrapper<>();
        qw.eq(EbBarterCategory::getStatus, 1);
        qw.orderByAsc(EbBarterCategory::getSort);
        qw.orderByAsc(EbBarterCategory::getId);
        return list(qw);
    }
}
