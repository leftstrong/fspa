package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.barter.EbBarterReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * 换物举报 Mapper 接口
 */
@Mapper
public interface BarterReportDao extends BaseMapper<EbBarterReport> {

}
