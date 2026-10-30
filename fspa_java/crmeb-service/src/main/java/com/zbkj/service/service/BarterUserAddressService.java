package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.barter.EbBarterUserAddress;

import java.util.List;

/**
 * 换物收货地址 业务接口
 */
public interface BarterUserAddressService extends IService<EbBarterUserAddress> {

    /** 我的地址列表（默认地址优先） */
    List<EbBarterUserAddress> myList(Integer uid);

    /** 新增地址（isDefault=1 时把该用户其他地址降为非默认） */
    Boolean add(Integer uid, EbBarterUserAddress address);

    /** 修改地址（校验归属） */
    Boolean edit(Integer uid, EbBarterUserAddress address);

    /** 删除地址（校验归属） */
    Boolean delete(Integer uid, Integer id);

    /** 设为默认地址 */
    Boolean setDefault(Integer uid, Integer id);
}
