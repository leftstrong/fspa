package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.barter.EbBarterMemberLevel;

import java.util.List;

/**
 * 换物会员等级 业务接口
 * 等级由「成功交换次数」自动匹配，作信用分段位外显
 */
public interface BarterMemberLevelService extends IService<EbBarterMemberLevel> {

    /** 全部等级（后台配置/排序用） */
    List<EbBarterMemberLevel> tree();

    /** 根据成功交换次数计算应得等级ID */
    Integer matchLevelId(Integer exchangeCount);

    /** 用户完成交换后重算等级并更新冗余字段 barter_level_id */
    Boolean recomputeUserLevel(Integer uid, Integer exchangeCount);

    /**
     * 删除等级
     * 若该等级下仍有用户，会按各自当前成功交换次数重新匹配等级后再删除，
     * 避免 user.barter_level_id 指向已删除的等级（孤儿引用）。
     * 兜底默认等级不允许删除。
     */
    Boolean deleteLevel(Integer id);
}
