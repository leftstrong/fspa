package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.barter.EbBarterOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 换物单 Mapper 接口
 */
@Mapper
public interface BarterOrderDao extends BaseMapper<EbBarterOrder> {

}
