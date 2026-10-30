package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.barter.EbBarterUserAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 换物收货地址 Mapper 接口
 */
@Mapper
public interface BarterUserAddressDao extends BaseMapper<EbBarterUserAddress> {

}
