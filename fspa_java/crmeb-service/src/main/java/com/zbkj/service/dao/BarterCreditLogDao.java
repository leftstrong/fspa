package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.barter.EbBarterCreditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 换物信用分日志 Mapper 接口
 */
@Mapper
public interface BarterCreditLogDao extends BaseMapper<EbBarterCreditLog> {

}
