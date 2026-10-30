package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.constants.BarterConstants;
import com.zbkj.common.model.barter.EbBarterEvaluate;
import com.zbkj.common.model.user.User;
import com.zbkj.service.dao.BarterEvaluateDao;
import com.zbkj.service.service.BarterEvaluateService;
import com.zbkj.service.service.BarterCreditLogService;
import com.zbkj.service.service.BarterConfigService;
import com.zbkj.service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;

/**
 * 换物互评 接口实现类
 * 互评影响被评人信用分：好评 +2 / 差评 -3（中性不变）
 */
@Service("barterEvaluateService")
public class BarterEvaluateServiceImpl extends ServiceImpl<BarterEvaluateDao, EbBarterEvaluate> implements BarterEvaluateService {

    @Autowired
    private UserService userService;
    @Autowired
    private BarterCreditLogService creditLogService;
    @Autowired
    private BarterConfigService barterConfigService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public Boolean addEvaluate(EbBarterEvaluate evaluate) {
        Integer uid = userService.getUserIdException();
        evaluate.setUid(uid);
        evaluate.setCreateTime(new Date());
        Integer toUid = evaluate.getToUid();
        Integer star = evaluate.getStar() == null ? 5 : evaluate.getStar();
        Boolean ok = transactionTemplate.execute(e -> {
            baseMapper.insert(evaluate);
            User to = userService.getById(toUid);
            if (to != null) {
                int delta = 0;
                String mark = "";
                if (star >= 4) {
                    // 好评加分值后台可配（barter_credit_good）
                    delta = barterConfigService.creditGood();
                    mark = "收到好评，信用 +" + delta;
                } else if (star <= 2) {
                    delta = -barterConfigService.creditBad();
                    mark = "收到差评，信用 " + delta;
                }
                if (delta != 0) {
                    try {
                        // 统一走流水：既改余额也留痕，供「信用分明细」页查询
                        creditLogService.changeCredit(toUid, delta, mark, 2, evaluate.getOrderId());
                    } catch (Exception ignore) {
                        int credit = to.getCreditScore() == null ? 0 : to.getCreditScore();
                        to.setCreditScore(Math.max(0, credit + delta));
                        userService.updateById(to);
                    }
                }
            }
            return Boolean.TRUE;
        });
        return ok != null && ok;
    }

    @Override
    public List<EbBarterEvaluate> listByToUid(Integer toUid) {
        return baseMapper.selectList(new QueryWrapper<EbBarterEvaluate>().eq("to_uid", toUid).orderByDesc("id"));
    }
}
