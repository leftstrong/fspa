package com.zbkj.service.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.barter.EbBarterItem;
import com.zbkj.common.model.barter.EbBarterUserAddress;
import com.zbkj.service.dao.BarterItemDao;
import com.zbkj.service.dao.BarterUserAddressDao;
import com.zbkj.service.service.BarterUserAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 换物收货地址 接口实现类
 */
@Service("barterUserAddressService")
public class BarterUserAddressServiceImpl extends ServiceImpl<BarterUserAddressDao, EbBarterUserAddress> implements BarterUserAddressService {

    @Autowired
    private BarterItemDao barterItemDao;

    @Override
    public List<EbBarterUserAddress> myList(Integer uid) {
        return baseMapper.selectList(new QueryWrapper<EbBarterUserAddress>()
                .eq("uid", uid).orderByDesc("is_default").orderByDesc("id"));
    }

    @Override
    public Boolean add(Integer uid, EbBarterUserAddress address) {
        if (address == null) throw new CrmebException("参数不完整");
        if (isBlank(address.getName())) throw new CrmebException("请填写收货人姓名");
        if (isBlank(address.getPhone())) throw new CrmebException("请填写收货人电话");
        if (isBlank(address.getDetail())) throw new CrmebException("请填写详细地址");
        address.setId(null);
        address.setUid(uid);
        address.setIsDefault(address.getIsDefault() == null ? 0 : address.getIsDefault());
        address.setCreateTime(DateUtil.date());
        address.setUpdateTime(DateUtil.date());
        boolean ok = save(address);
        if (ok && Integer.valueOf(1).equals(address.getIsDefault())) clearOtherDefault(uid, address.getId());
        return ok;
    }

    @Override
    public Boolean edit(Integer uid, EbBarterUserAddress address) {
        if (address == null || address.getId() == null) throw new CrmebException("参数不完整");
        EbBarterUserAddress db = baseMapper.selectById(address.getId());
        if (db == null || !uid.equals(db.getUid())) throw new CrmebException("地址不存在");
        // 只允许修改业务字段，uid 以数据库为准
        db.setName(address.getName());
        db.setPhone(address.getPhone());
        db.setProvince(address.getProvince());
        db.setCity(address.getCity());
        db.setDistrict(address.getDistrict());
        db.setDetail(address.getDetail());
        db.setUpdateTime(DateUtil.date());
        if (address.getIsDefault() != null) db.setIsDefault(address.getIsDefault());
        boolean ok = updateById(db);
        if (ok && Integer.valueOf(1).equals(db.getIsDefault())) clearOtherDefault(uid, db.getId());
        return ok;
    }

    @Override
    public Boolean delete(Integer uid, Integer id) {
        EbBarterUserAddress db = baseMapper.selectById(id);
        if (db == null || !uid.equals(db.getUid())) throw new CrmebException("地址不存在");
        // 快递交付保护：若存在「在售 + 快递」商品，必须至少保留一个默认收货地址
        if (hasOnSaleExpressItem(uid)) {
            int remain = baseMapper.selectCount(new QueryWrapper<EbBarterUserAddress>().eq("uid", uid)).intValue();
            if (remain <= 1) {
                // 删掉就没有任何地址了，而快递商品仍需默认地址 → 拦截
                throw new CrmebException("您有在售的快递交付商品，必须保留至少一个收货地址");
            }
            if (Integer.valueOf(1).equals(db.getIsDefault())) {
                // 删的是默认地址且仍有其他地址：把最近一条提升为默认，维持「必有默认地址」约束
                EbBarterUserAddress next = baseMapper.selectOne(new QueryWrapper<EbBarterUserAddress>()
                        .eq("uid", uid).ne("id", id).orderByDesc("id").last("LIMIT 1"));
                if (next != null) {
                    next.setIsDefault(1);
                    next.setUpdateTime(DateUtil.date());
                    baseMapper.updateById(next);
                }
            }
        }
        return removeById(id);
    }

    /** 是否存在在售且为快递交付方式的商品（uid 维度） */
    private boolean hasOnSaleExpressItem(Integer uid) {
        Integer c = barterItemDao.selectCount(new QueryWrapper<EbBarterItem>()
                .eq("uid", uid).eq("status", 1).eq("delivery_type", 2));
        return c != null && c > 0;
    }

    @Override
    public Boolean setDefault(Integer uid, Integer id) {
        EbBarterUserAddress db = baseMapper.selectById(id);
        if (db == null || !uid.equals(db.getUid())) throw new CrmebException("地址不存在");
        db.setIsDefault(1);
        db.setUpdateTime(DateUtil.date());
        boolean ok = updateById(db);
        if (ok) clearOtherDefault(uid, id);
        return ok;
    }

    /** 把该用户其他地址降为非默认（保证唯一默认） */
    private void clearOtherDefault(Integer uid, Integer keepId) {
        EbBarterUserAddress upd = new EbBarterUserAddress();
        upd.setIsDefault(0);
        update(upd, new QueryWrapper<EbBarterUserAddress>()
                .eq("uid", uid).ne("id", keepId).eq("is_default", 1));
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
