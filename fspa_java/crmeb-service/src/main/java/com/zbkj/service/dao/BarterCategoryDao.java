package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.barter.EbBarterCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 换物品类 Mapper 接口
 */
@Mapper
public interface BarterCategoryDao extends BaseMapper<EbBarterCategory> {

}
