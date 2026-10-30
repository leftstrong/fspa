package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.barter.EbBarterMemberLevel;
import com.zbkj.common.model.user.User;
import com.zbkj.service.dao.BarterMemberLevelDao;
import com.zbkj.service.service.BarterMemberLevelService;
import com.zbkj.service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 换物会员等级 接口实现类
 * 等级由「成功交换次数」自动匹配，作信用分段位外显
 */
@Service("barterMemberLevelService")
public class BarterMemberLevelServiceImpl extends ServiceImpl<BarterMemberLevelDao, EbBarterMemberLevel> implements BarterMemberLevelService {

    /** 兜底等级ID：matchLevelId 未命中任何区间时返回该值，不允许删除 */
    private static final Integer DEFAULT_LEVEL_ID = 1;

    @Autowired
    private UserService userService;

    @Override
    public List<EbBarterMemberLevel> tree() {
        return baseMapper.selectList(new QueryWrapper<EbBarterMemberLevel>().orderByAsc("sort"));
    }

    @Override
    public Integer matchLevelId(Integer exchangeCount) {
        if (exchangeCount == null) exchangeCount = 0;
        // 从高阈值往低匹配，命中第一个满足 min<=count<max 的等级
        List<EbBarterMemberLevel> list = baseMapper.selectList(new QueryWrapper<EbBarterMemberLevel>().orderByDesc("min_count"));
        for (EbBarterMemberLevel level : list) {
            int max = (level.getMaxCount() == null || level.getMaxCount() == 0) ? Integer.MAX_VALUE : level.getMaxCount();
            if (exchangeCount >= level.getMinCount() && exchangeCount < max) {
                return level.getId();
            }
        }
        return DEFAULT_LEVEL_ID;
    }

    @Override
    public Boolean recomputeUserLevel(Integer uid, Integer exchangeCount) {
        User user = userService.getById(uid);
        if (user == null) return false;
        user.setBarterLevelId(matchLevelId(exchangeCount));
        userService.updateById(user);
        return true;
    }

    @Override
    public Boolean deleteLevel(Integer id) {
        if (id == null || id <= 0) return false;
        // 兜底等级：matchLevelId 未命中任何区间时返回该值，删除后会导致用户无等级
        if (DEFAULT_LEVEL_ID.equals(id)) {
            throw new CrmebException("默认等级不可删除");
        }
        EbBarterMemberLevel level = baseMapper.selectById(id);
        if (level == null) {
            throw new CrmebException("等级不存在");
        }
        // 该等级下仍有用户：先按各自成功交换次数重算等级，再删除，避免孤儿引用
        List<User> users = userService.list(new QueryWrapper<User>().eq("barter_level_id", id));
        if (users != null && !users.isEmpty()) {
            for (User user : users) {
                Integer count = user.getBarterExchangeCount() == null ? 0 : user.getBarterExchangeCount();
                recomputeUserLevel(user.getUid(), count);
            }
        }
        return baseMapper.deleteById(id) > 0;
    }
}
