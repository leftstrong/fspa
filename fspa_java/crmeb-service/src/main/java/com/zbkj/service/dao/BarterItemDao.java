package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.barter.EbBarterItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 闲置物品 Mapper 接口
 */
@Mapper
public interface BarterItemDao extends BaseMapper<EbBarterItem> {

    /**
     * 原子增减「我想要」人数（GREATEST 兜底防止并发下出现负数）
     */
    @Update("UPDATE eb_barter_item SET want_count = GREATEST(want_count + #{delta}, 0) WHERE id = #{id}")
    int incrWantCount(@Param("id") Integer id, @Param("delta") Integer delta);
}
