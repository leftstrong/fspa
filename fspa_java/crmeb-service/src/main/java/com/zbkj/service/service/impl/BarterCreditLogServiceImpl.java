package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.barter.EbBarterCreditLog;
import com.zbkj.common.model.user.User;
import com.zbkj.service.dao.BarterCreditLogDao;
import com.zbkj.service.service.BarterCreditLogService;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;

/**
 * 换物信用分日志 接口实现
 * 统一入口：加/扣信用分 + 写日志 + 维护用户信用分余额，返回最新信用分
 */
@Service("barterCreditLogService")
public class BarterCreditLogServiceImpl extends ServiceImpl<BarterCreditLogDao, EbBarterCreditLog> implements BarterCreditLogService {

    @Autowired
    private UserService userService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public Integer changeCredit(Integer uid, Integer delta, String mark, Integer linkType, Integer linkId) {
        if (uid == null || delta == null || delta == 0) return null;
        User user = userService.getById(uid);
        if (user == null) return null;
        int current = user.getCreditScore() == null ? 0 : user.getCreditScore();
        int finalScore = Math.max(0, current + delta); // 不为负
        Integer finalBalance = finalScore;
        Boolean ok = transactionTemplate.execute(e -> {
            user.setCreditScore(finalBalance);
            userService.updateById(user);

            EbBarterCreditLog log = new EbBarterCreditLog();
            log.setUid(uid);
            log.setType(delta > 0 ? 1 : 2);
            log.setValue(Math.abs(delta));
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
    public CommonPage<EbBarterCreditLog> myLog(Integer uid, PageParamRequest pageRequest) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EbBarterCreditLog> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        qw.eq(EbBarterCreditLog::getUid, uid).orderByDesc(EbBarterCreditLog::getCreateTime)
                .orderByDesc(EbBarterCreditLog::getId);
        com.baomidou.mybatisplus.core.metadata.IPage<EbBarterCreditLog> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        pageRequest.getPage(), pageRequest.getLimit());
        com.baomidou.mybatisplus.core.metadata.IPage<EbBarterCreditLog> res = baseMapper.selectPage(page, qw);
        // 本项目未注册 MP 分页插件，selectPage 不执行 count 查询（total 恒为 0），这里自己 count
        long total = baseMapper.selectCount(qw);
        long size = pageRequest.getLimit() > 0 ? pageRequest.getLimit() : 20;
        CommonPage<EbBarterCreditLog> cp = new CommonPage<>();
        cp.setList(res.getRecords());
        cp.setTotal(total);
        cp.setPage((int) res.getCurrent());
        cp.setLimit((int) size);
        cp.setTotalPage(size == 0 ? 0 : (int) ((total + size - 1) / size));
        return cp;
    }
}
