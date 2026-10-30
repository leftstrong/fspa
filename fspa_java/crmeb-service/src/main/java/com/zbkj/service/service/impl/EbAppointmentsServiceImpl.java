package com.zbkj.service.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.aliyuncs.exceptions.ClientException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.constants.DateFromatConstants;
import com.zbkj.common.model.article.EbAppointmentsEntity;
import com.zbkj.common.model.article.EbArtisanEntity;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.system.SystemStore;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.AppointmentDetailResponse;
import com.zbkj.common.response.AppointmentSearchRequest;
import com.zbkj.common.sms.AbstractSmsDTO;
import com.zbkj.common.sms.dto.ArtisanNoticeSmsDTO;
import com.zbkj.common.sms.dto.CreateOrderNoticeSmsDTO;
import com.zbkj.common.sms.provide.SmsProvider;
import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.common.utils.DateUtil;
import com.zbkj.common.vo.dateLimitUtilVo;
import com.zbkj.service.dao.EbAppointmentsDao;
import com.zbkj.service.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 预约表 接口实现类
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
 * @date Thu May 18 08:22:55 CST 2023
 * +----------------------------------------------------------------------
 * @email stivepeim@outlook.com
 * +----------------------------------------------------------------------
 */

@Slf4j
@Service("ebAppointmentsService")
public class EbAppointmentsServiceImpl extends ServiceImpl<EbAppointmentsDao, EbAppointmentsEntity> implements EbAppointmentsService {
    @Resource
    private EbAppointmentsDao appointmentsDao;
    @Autowired
    private SystemStoreService storeService;
    @Autowired
    private UserService userService;
    @Autowired
    private EbArtisanService artisanService;
    @Autowired
    private StoreProductService storeProductService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public CommonPage<AppointmentDetailResponse> adminList(AppointmentSearchRequest request, PageParamRequest pageParamRequest) {
        Page<Object> startPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        QueryWrapper<EbAppointmentsEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Objects.nonNull(request.getStoreId()), "store_id", request.getStoreId());
        queryWrapper.eq(!StringUtils.isBlank(request.getVerifyCode()), "verify_code", request.getVerifyCode());
        queryWrapper.orderByDesc("id");
        if (StringUtils.isNotBlank(request.getDateLimit())) {
            dateLimitUtilVo dateLimit = DateUtil.getDateLimit(request.getDateLimit());
            queryWrapper.between("appointment_date", dateLimit.getStartTime(), dateLimit.getEndTime());
        }
        List<EbAppointmentsEntity> artisanList = appointmentsDao.selectList(queryWrapper);
        List<AppointmentDetailResponse> detailResponseList = new ArrayList<>();
        AppointmentDetailResponse temp = null;
        for (EbAppointmentsEntity appointmentsEntity : artisanList) {
            temp = new AppointmentDetailResponse();
            BeanUtils.copyProperties(appointmentsEntity, temp);
            temp.setStoreName(storeService.getById(temp.getStoreId()).getName());
            temp.setStatusName(temp.getStatus().equals(0) ? "已取消" : "正常");
            temp.setUName(userService.getById(temp.getUid()).getNickname());
            temp.setArtisanName("-");
            temp.setProductNames("-");
            if (Objects.nonNull(temp.getArtisanId())) {
                temp.setArtisanName(artisanService.getById(temp.getArtisanId()).getNickname());
            }
            if (!StringUtils.isBlank(temp.getProductIds())) {
                List<String> prodouctIds = Arrays.asList(temp.getProductIds().split(","));
                List<Integer> intList = new ArrayList<>();
                for (String str : prodouctIds) {
                    int value = Integer.parseInt(str);
                    intList.add(value);
                }
                List<StoreProduct> listInIds = storeProductService.getListInIds(intList);
                StringBuffer productName = new StringBuffer("");
                for (StoreProduct product : listInIds) {
                    productName.append(product.getStoreName()).append("/");
                }
                temp.setProductNames(productName.toString());
            }
            //商品名称
            detailResponseList.add(temp);
        }
        return CommonPage.restPage(CommonPage.copyPageInfo(startPage, detailResponseList));
    }

    @Override
    public List<AppointmentDetailResponse> myList(PageParamRequest pageRequest) {
        Integer userId = userService.getUserIdException();
        PageHelper.startPage(pageRequest.getPage(), pageRequest.getLimit());
        QueryWrapper<EbAppointmentsEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", userId);
        queryWrapper.orderByDesc("id");
        List<EbAppointmentsEntity> appointmentsList = appointmentsDao.selectList(queryWrapper);
        List<AppointmentDetailResponse> detailResponseList = new ArrayList<>();
        AppointmentDetailResponse temp = null;
        if (CollectionUtils.isEmpty(appointmentsList)) {
            return Collections.EMPTY_LIST;
        }
        Set<Integer> storeIdSet = appointmentsList.stream().map(EbAppointmentsEntity::getStoreId).collect(Collectors.toSet());
        Set<Integer> uIdSet = appointmentsList.stream().map(EbAppointmentsEntity::getUid).collect(Collectors.toSet());
        Set<Integer> artisanIdSet = appointmentsList.stream().map(EbAppointmentsEntity::getArtisanId).collect(Collectors.toSet());

        Map<Integer, SystemStore> storeMap = storeService.listByIds(storeIdSet).stream().collect(Collectors.toMap(s -> s.getId(), Function.identity()));
        Map<Integer, User> userMap = userService.listByIds(uIdSet).stream().collect(Collectors.toMap(s -> s.getUid(), Function.identity()));
        Map<Integer, EbArtisanEntity> artisanMap = artisanService.listByIds(artisanIdSet).stream().collect(Collectors.toMap(s -> s.getId(), Function.identity()));

        for (EbAppointmentsEntity appointmentsEntity : appointmentsList) {
            temp = new AppointmentDetailResponse();
            BeanUtils.copyProperties(appointmentsEntity, temp);
            temp.setStoreName(storeMap.containsKey(temp.getStoreId()) ? storeMap.get(temp.getStoreId()).getName(): "");
            temp.setStatusName(temp.getStatus().equals(0) ? "已取消" : "正常");
            temp.setUName(userMap.containsKey(temp.getUid()) ? userMap.get(temp.getUid()).getNickname(): "");
            temp.setArtisanName("-");
            temp.setProductNames("-");
            temp.setAppointmentDateStr(cn.hutool.core.date.DateUtil.format(appointmentsEntity.getAppointmentDate(), DateFromatConstants.DATE_FORMAT));
            if (Objects.nonNull(temp.getArtisanId())) {
                temp.setArtisanName(artisanMap.containsKey(temp.getArtisanId()) ? artisanMap.get(temp.getArtisanId()).getNickname(): "");
            }
            if (!StringUtils.isBlank(temp.getProductIds())) {
                List<String> prodouctIds = Arrays.asList(temp.getProductIds().split(","));
                List<Integer> intList = new ArrayList<>();
                for (String str : prodouctIds) {
                    int value = Integer.parseInt(str);
                    intList.add(value);
                }
                List<StoreProduct> listInIds = storeProductService.getListInIds(intList);
                StringBuffer productName = new StringBuffer("");
                for (StoreProduct product : listInIds) {
                    productName.append(product.getStoreName()).append("/");
                }
                temp.setProductNames(productName.toString());
            }
            //商品名称
            detailResponseList.add(temp);
        }
        return detailResponseList;
    }

    @Override
    public AppointmentDetailResponse getInfo(Integer id) {
        EbAppointmentsEntity appointmentsEntity = appointmentsDao.selectById(id);
        if (Objects.isNull(appointmentsEntity)) {
            return null;
        }
        AppointmentDetailResponse temp = new AppointmentDetailResponse();
        BeanUtils.copyProperties(appointmentsEntity, temp);
        temp.setStoreName(storeService.getById(temp.getStoreId()).getName());
        temp.setStatusName(temp.getStatus().equals(0) ? "已取消" : "正常");
        temp.setUName(userService.getById(temp.getUid()).getNickname());
        temp.setArtisanName("-");
        temp.setProductNames("-");
        temp.setAppointmentDateStr(cn.hutool.core.date.DateUtil.format(appointmentsEntity.getAppointmentDate(), DateFromatConstants.DATE_FORMAT));
        if (Objects.nonNull(temp.getArtisanId())) {
            temp.setArtisanName(artisanService.getById(temp.getArtisanId()).getNickname());
        }
        if (!StringUtils.isBlank(temp.getProductIds())) {
            List<String> prodouctIds = Arrays.asList(temp.getProductIds().split(","));
            List<Integer> intList = new ArrayList<>();
            for (String str : prodouctIds) {
                int value = Integer.parseInt(str);
                intList.add(value);
            }
            List<StoreProduct> listInIds = storeProductService.getListInIds(intList);
            StringBuffer productName = new StringBuffer("");
            for (StoreProduct product : listInIds) {
                productName.append(product.getStoreName()).append("/");
            }
            temp.setProductNames(productName.toString());
        }
        return temp;
    }

    @Override
    public Boolean create(EbAppointmentsEntity appointmentsEntity) {
        //参数 storeId artisanId appointmentDate  productIds 页面传过来
        Integer userId = userService.getUserIdException();
        appointmentsEntity.setStatus(1);
        appointmentsEntity.setUid(userId);
        appointmentsEntity.setVerifyCode(CrmebUtil.randomCount(1111111111, 999999999) + "");
        Boolean execute = transactionTemplate.execute(e -> {
            appointmentsDao.insert(appointmentsEntity);
            return Boolean.TRUE;
        });
        Integer artisanId = appointmentsEntity.getArtisanId();
        if (Objects.isNull(artisanId)) {
            //默认丸子老师
            artisanId = 16;
        }
        EbArtisanEntity artisanTemp = artisanService.getById(artisanId);
        //发送通知短信
        if (Objects.nonNull(artisanTemp)) {
            sendSms(new ArtisanNoticeSmsDTO()
                    .setReal_name(artisanTemp.getRealName())
                    .setAppointment_date(DateUtil.dateToStr(appointmentsEntity.getAppointmentDate(),
                    "yyyy-MM-dd HH:mm")).setTelephone(artisanTemp.getPhone()));
        }
        return execute;
    }

    @Override
    public Boolean delete(Integer id) {
        Boolean execute = transactionTemplate.execute(e -> {
            appointmentsDao.deleteById(id);
            return Boolean.TRUE;
        });
        return execute;
    }

    @Override
    public Boolean cancel(Integer id) {
        EbAppointmentsEntity appointmentsEntity = appointmentsDao.selectById(id);
        //0: 已取消 1:正常
        appointmentsEntity.setStatus(0);
        Boolean execute = transactionTemplate.execute(e -> {
            appointmentsDao.updateById(appointmentsEntity);
            return Boolean.TRUE;
        });
        return execute;
    }

    @Override
    public List<Map<String, Object>> getWorkDayTimeList() {
        //门店非营业时间2023-06-01
        Set<String> shopWorkOutSet = new HashSet<>();
        List<SystemStore> shopList = storeService.list();
        for (SystemStore object : shopList) {
            if (StringUtils.isEmpty(object.getDayOut())) {
                continue;
            }
            String[] workOutArray = object.getDayOut().split(",");
            for (String s : workOutArray) {
                shopWorkOutSet.add(s);
            }
        }
        List<Map<String,Object>> resultList=new ArrayList<>();
        //预约时间段
        List<String> timeList = new ArrayList<>(Arrays.asList("10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00"));
        String nowTime = DateUtil.dateToStr(new Date(), "HH");
        List<String> numberDays = DateUtil.getNumberDays(7);
        for (int i = 0; i < numberDays.size(); i++) {
            String numberDay = numberDays.get(i);
            Map<String, Object> workDayTimeMap = new HashMap<>();
            if (shopWorkOutSet.contains(numberDay)) {
                workDayTimeMap.put(numberDay, Collections.EMPTY_LIST);
                continue;
            }
            if (i == 0) {
                //当天
                List<String> tempList = new ArrayList<>();
                for (String time : timeList) {
                    String temp = time.replace(":00","");
                    if (Integer.parseInt(temp) > Integer.parseInt(nowTime)) {
                        tempList.add(time);
                    }
                }
                if (CollectionUtil.isEmpty(tempList)) {
                    workDayTimeMap.put(numberDay, Collections.EMPTY_LIST);
                    continue;
                }
                workDayTimeMap.put("date", numberDay);
                workDayTimeMap.put("timeList", tempList);
                resultList.add(workDayTimeMap);
                continue;
            }
            workDayTimeMap.put("date", numberDay);
            workDayTimeMap.put("timeList", timeList);
            resultList.add(workDayTimeMap);
        }

        return resultList;
    }

    /**
     * 短信发送
     * @param dto
     */
    private void sendSms(AbstractSmsDTO dto) {
        Object data = null;
        try {
            SmsProvider provider = SmsProvider.smsProviderMap.get(SmsProvider.SMS_PROVIDER_ALIYUN);
            String templateCode = provider.getTemplateCode(dto.getBusinessType());
            if (StringUtils.isEmpty(templateCode)) {
                log.error("当前短信运用上不存在该业务的短信模板, 短信供应商：[{}]， 业务类型：[{}]", provider.getName(), dto.getBusinessType());
                return;
            }
            Map<String, String> map = provider.getMsgMap(dto);
            data = provider.send(dto.getTelephone(), templateCode, map);
            log.info("aliyunSms response: {}", data.toString());
        }catch (ClientException clientEx){
            log.error("aliyunSms ClientException:Exception: {}" ,clientEx);
            log.error("aliyunSms ClientException:data: {}" ,data == null ? "data returns null" : JSON.toJSONString(data));
        } catch (Exception e) {
            log.error("短信发送异常 e: {}", e);
        }
    }
}
