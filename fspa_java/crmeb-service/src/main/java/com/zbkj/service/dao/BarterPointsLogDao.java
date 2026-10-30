package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.barter.EbBarterPointsLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 换物积分日志 Mapper 接口
 */
@Mapper
public interface BarterPointsLogDao extends BaseMapper<EbBarterPointsLog> {

}
