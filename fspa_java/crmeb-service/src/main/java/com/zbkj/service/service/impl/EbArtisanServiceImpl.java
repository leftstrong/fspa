package com.zbkj.service.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qcloud.cos.utils.Jackson;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.article.EbAppointmentsEntity;
import com.zbkj.common.model.article.EbArtisanEntity;
import com.zbkj.common.model.system.SystemStore;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.ArtisanDetailResponse;
import com.zbkj.common.response.ArtisanSearchRequest;
import com.zbkj.common.utils.DateUtil;
import com.zbkj.service.dao.EbArtisanDao;
import com.zbkj.service.service.EbAppointmentsService;
import com.zbkj.service.service.EbArtisanService;
import com.zbkj.service.service.SystemStoreService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 匠人表 接口实现类
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2022 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 *
 * @author: 大粽子
 * +----------------------------------------------------------------------
 * @date Thu May 18 08:44:58 CST 2023
 * +----------------------------------------------------------------------
 * @email stivepeim@outlook.com
 * +----------------------------------------------------------------------
 */

@Service("ebArtisanService")
public class EbArtisanServiceImpl extends ServiceImpl<EbArtisanDao, EbArtisanEntity> implements EbArtisanService {
    @Resource
    private EbArtisanDao artisanDao;
    @Autowired
    private SystemStoreService storeService;
    @Autowired
    private EbAppointmentsService appointmentsService;

    @Override
    public CommonPage<ArtisanDetailResponse> adminList(ArtisanSearchRequest request, PageParamRequest pageParamRequest) {
        Page<Object> startPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        QueryWrapper<EbArtisanEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Objects.nonNull(request.getStoreId()), "store_id", request.getStoreId());
        queryWrapper.likeLeft(Objects.nonNull(request.getPhone()), "phone", request.getPhone());
        queryWrapper.orderByDesc("id");
        List<EbArtisanEntity> artisanList = artisanDao.selectList(queryWrapper);
        List<ArtisanDetailResponse> detailResponseList = new ArrayList<>();
        ArtisanDetailResponse temp = null;
        for (EbArtisanEntity ebArtisanEntity : artisanList) {
            temp = new ArtisanDetailResponse();
            BeanUtils.copyProperties(ebArtisanEntity, temp);
            temp.setStoreName(storeService.getById(temp.getStoreId()).getName());
            temp.setStatusName(temp.getStatus().equals(0) ? "禁止" : "正常");
            detailResponseList.add(temp);
        }
        return CommonPage.restPage(CommonPage.copyPageInfo(startPage, detailResponseList));
    }

    @Override
    public List<ArtisanDetailResponse> listByAppointmentTime(String appointmentTime) {
        if (StringUtils.isBlank(appointmentTime)) {
            return Collections.emptyList();
        }
        QueryWrapper<EbArtisanEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        List<EbArtisanEntity> artisanList = artisanDao.selectList(queryWrapper);
        Map<Integer, SystemStore> storeMap = storeService.list().stream().collect(Collectors.toMap(s -> s.getId(), Function.identity()));
        List<ArtisanDetailResponse> detailResponseList = new ArrayList<>();
        ArtisanDetailResponse temp = null;
        for (EbArtisanEntity ebArtisanEntity : artisanList) {
            if (StringUtils.isBlank(ebArtisanEntity.getWorkTime())) {
                continue;
            }
            //排班set
            Set<String> workTimeSet = getWorkTimeSet(ebArtisanEntity.getWorkTime());
            if (!workTimeSet.contains(appointmentTime)) {
                continue;
            }
            //该时段是否已被预约
            List<EbAppointmentsEntity> tempList = appointmentsService.list(Wrappers.<EbAppointmentsEntity>lambdaQuery()
                    .eq(EbAppointmentsEntity::getArtisanId, ebArtisanEntity.getId())
                    .eq(EbAppointmentsEntity::getAppointmentDate, DateUtil.strToDate(appointmentTime, "yyyy-MM-dd HH:mm")));
            if (CollectionUtil.isNotEmpty(tempList)) {
                continue;
            }
            temp = new ArtisanDetailResponse();
            BeanUtils.copyProperties(ebArtisanEntity, temp);
            temp.setStoreName(storeMap.containsKey(ebArtisanEntity.getStoreId()) ? storeMap.get(ebArtisanEntity.getStoreId()).getName() : "-");
            detailResponseList.add(temp);
        }
        return detailResponseList;
    }

    @Override
    public ArtisanDetailResponse getCraftsmanInfoById(Integer craftsmanId) {
        EbArtisanEntity ebArtisanEntity = artisanDao.selectById(craftsmanId);
        Optional.ofNullable(ebArtisanEntity).orElseThrow(() -> new CrmebException("该手艺人不存在"));
        ArtisanDetailResponse artisanDetailResponse = new ArtisanDetailResponse();
        BeanUtils.copyProperties(ebArtisanEntity, artisanDetailResponse);
        return artisanDetailResponse;
    }

    /**
     * 排版列表
     *
     * @param workTime
     * @return
     */
    private Set<String> getWorkTimeSet(String workTime) {
        Set<String> workTimeSet = new HashSet<>();
        Map<String, List<String>> workTimeMap = Jackson.fromJsonString(workTime, Map.class);
        for (String day : workTimeMap.keySet()) {
            List<String> times = workTimeMap.get(day);
            for (String time : times) {
                workTimeSet.add(day + " " + time);
            }
        }
        return workTimeSet;
    }
//    /**
//     * EbArtisan列表查询
//     * @param request 默认是是体类 根据自己需求修改或者创建自己的request
//     * @param pageParamRequest 分页参数对象
//     * @return
//     */
//    @Override
//    public PageUtils queryPage(EbArtisanEntity request, PageParamRequest pageParamRequest) {
//        PageHelper.startPage(pageParamRequest.getPageNum(), pageParamRequest.getPageSize());
//
//        //列表查询 EbArtisan 类的多条件查询
//        LambdaQueryWrapper<EbArtisanEntity> lambdaQueryWrapper = new LambdaQueryWrapper<>();
//        EbArtisanEntity model = new EbArtisanEntity();
//        BeanUtils.copyProperties(request, model);
//        lambdaQueryWrapper.setEntity(model);
//        return dao.selectList(lambdaQueryWrapper);
//    }

}
