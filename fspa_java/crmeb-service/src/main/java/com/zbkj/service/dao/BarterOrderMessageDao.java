package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.barter.EbBarterOrderMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BarterOrderMessageDao extends BaseMapper<EbBarterOrderMessage> {
}
