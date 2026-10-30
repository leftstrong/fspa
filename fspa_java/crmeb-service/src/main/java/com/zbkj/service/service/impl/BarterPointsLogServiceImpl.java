package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.barter.EbBarterPointsLog;
import com.zbkj.common.model.user.User;
import com.zbkj.service.dao.BarterPointsLogDao;
import com.zbkj.service.service.BarterPointsLogService;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.page.CommonPage;
import com.zbkj.service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;

/**
 * 换物积分日志 接口实现类
 * 统一入口：扣/加积分 + 写日志 + 维护用户积分余额，返回最新余额（不足返回 null）
 */
@Service("barterPointsLogService")
public class BarterPointsLogServiceImpl extends ServiceImpl<BarterPointsLogDao, EbBarterPointsLog> implements BarterPointsLogService {

    @Autowired
    private UserService userService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public Integer changePoints(Integer uid, Integer type, Integer value, String mark, Integer linkType, Integer linkId) {
        if (value == null || value <= 0) return null;
        User user = userService.getById(uid);
        if (user == null) return null;
        int balance = user.getBarterPoints() == null ? 0 : user.getBarterPoints();
        if (type == 2) { // 消费
            if (balance < value) return null; // 积分不足
            balance -= value;
        } else { // 获取
            balance += value;
        }
        Integer finalBalance = balance;
        Boolean ok = transactionTemplate.execute(e -> {
            user.setBarterPoints(finalBalance);
            userService.updateById(user);

            EbBarterPointsLog log = new EbBarterPointsLog();
            log.setUid(uid);
            log.setType(type);
            log.setValue(value);
            log.setBalance(finalBalance);
            log.setMark(mark);
            log.setLinkType(linkType == null ? 0 : linkType);
            log.setLinkId(linkId == null ? 0 : linkId);
            log.setCreateTime(new Date());
            baseMapper.insert(log);
            return Boolean.TRUE;
        });
        return ok != null && ok ? finalBalance : null;
    }

    @Override
    public Integer freezePoints(Integer uid, Integer amount, String mark, Integer linkId) {
        if (amount == null || amount <= 0) return null;
        User user = userService.getById(uid);
        if (user == null) return null;
        int balance = user.getBarterPoints() == null ? 0 : user.getBarterPoints();
        int frozen = user.getBarterPointsFrozen() == null ? 0 : user.getBarterPointsFrozen();
        int real = Math.min(balance, amount); // 余额不足时按可用部分冻结，差额免收，不让用户变负分
        if (real <= 0) return null;
        int finalBalance = balance - real;
        int finalFrozen = frozen + real;
        Boolean ok = transactionTemplate.execute(e -> {
            user.setBarterPoints(finalBalance);
            user.setBarterPointsFrozen(finalFrozen);
            userService.updateById(user);
            baseMapper.insert(buildLog(uid, 2, real, finalBalance, mark, 5, linkId));
            return Boolean.TRUE;
        });
        return ok != null && ok ? finalBalance : null;
    }

    @Override
    public Integer unfreezePoints(Integer uid, Integer amount, String mark, Integer linkId) {
        if (amount == null || amount <= 0) return null;
        User user = userService.getById(uid);
        if (user == null) return null;
        int balance = user.getBarterPoints() == null ? 0 : user.getBarterPoints();
        int frozen = user.getBarterPointsFrozen() == null ? 0 : user.getBarterPointsFrozen();
        int real = Math.min(frozen, amount); // 只解冻真实冻结过的部分
        if (real <= 0) return null;
        int finalBalance = balance + real;
        int finalFrozen = frozen - real;
        Boolean ok = transactionTemplate.execute(e -> {
            user.setBarterPoints(finalBalance);
            user.setBarterPointsFrozen(finalFrozen);
            userService.updateById(user);
            baseMapper.insert(buildLog(uid, 1, real, finalBalance, mark, 5, linkId));
            return Boolean.TRUE;
        });
        return ok != null && ok ? finalBalance : null;
    }

    @Override
    public Integer settleFrozenPoints(Integer uid, Integer amount, String mark, Integer linkId) {
        if (amount == null || amount <= 0) return null;
        User user = userService.getById(uid);
        if (user == null) return null;
        int frozen = user.getBarterPointsFrozen() == null ? 0 : user.getBarterPointsFrozen();
        int real = Math.min(frozen, amount); // 冻结转实扣：不再动可用余额（发起时已扣过）
        if (real <= 0) return null;
        int finalFrozen = frozen - real;
        int finalBalance = user.getBarterPoints() == null ? 0 : user.getBarterPoints();
        Boolean ok = transactionTemplate.execute(e -> {
            user.setBarterPointsFrozen(finalFrozen);
            userService.updateById(user);
            baseMapper.insert(buildLog(uid, 2, real, finalBalance, mark, 5, linkId));
            return Boolean.TRUE;
        });
        return ok != null && ok ? finalBalance : null;
    }

    @Override
    public CommonPage<EbBarterPointsLog> myLog(Integer uid, PageParamRequest pageRequest) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EbBarterPointsLog> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        qw.eq(EbBarterPointsLog::getUid, uid).orderByDesc(EbBarterPointsLog::getCreateTime)
                .orderByDesc(EbBarterPointsLog::getId);
        com.baomidou.mybatisplus.core.metadata.IPage<EbBarterPointsLog> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        pageRequest.getPage(), pageRequest.getLimit());
        com.baomidou.mybatisplus.core.metadata.IPage<EbBarterPointsLog> res = baseMapper.selectPage(page, qw);
        // CommonPage 的 restPage 只接 SpringData/PageHelper 分页，MyBatis-Plus 的 IPage 这里手工组装
        // 本项目未注册 MP 分页插件，selectPage 不执行 count 查询（total 恒为 0），这里自己 count
        long total = baseMapper.selectCount(qw);
        long size = pageRequest.getLimit() > 0 ? pageRequest.getLimit() : 20;
        CommonPage<EbBarterPointsLog> cp = new CommonPage<>();
        cp.setList(res.getRecords());
        cp.setTotal(total);
        cp.setPage((int) res.getCurrent());
        cp.setLimit((int) size);
        cp.setTotalPage(size == 0 ? 0 : (int) ((total + size - 1) / size));
        return cp;
    }

    private EbBarterPointsLog buildLog(Integer uid, Integer type, Integer value, Integer balance,
                                       String mark, Integer linkType, Integer linkId) {
        EbBarterPointsLog log = new EbBarterPointsLog();
        log.setUid(uid);
        log.setType(type);
        log.setValue(value);
        log.setBalance(balance);
        log.setMark(mark);
        log.setLinkType(linkType == null ? 0 : linkType);
        log.setLinkId(linkId == null ? 0 : linkId);
        log.setCreateTime(new Date());
        return log;
    }
}
