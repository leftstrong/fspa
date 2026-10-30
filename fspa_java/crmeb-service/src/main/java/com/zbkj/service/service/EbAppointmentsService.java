package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.article.EbAppointmentsEntity;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.AppointmentDetailResponse;
import com.zbkj.common.response.AppointmentSearchRequest;

import java.util.List;
import java.util.Map;

/**
 * 预约表 业务接口
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
public interface EbAppointmentsService extends IService<EbAppointmentsEntity> {

    /**
     * EbArtisan 列表查询
     *
     * @param request          默认是是体类 根据自己需求修改或者创建自己的request
     * @param pageParamRequest 分页参数对象
     * @return
     */
    CommonPage<AppointmentDetailResponse> adminList(AppointmentSearchRequest request, PageParamRequest pageParamRequest);

    /**
     * 我的预约列表
     * @return
     * @param pageRequest
     */
    List<AppointmentDetailResponse> myList(PageParamRequest pageRequest);

    /**
     * 预约明细
     * @param id
     * @return
     */
    AppointmentDetailResponse getInfo(Integer id);

    /**
     * 新增预约
     * @param appointmentsEntity
     */
    Boolean create(EbAppointmentsEntity appointmentsEntity);

    /**
     * 删除预约
     * @param id
     * @return
     */
    Boolean delete(Integer id);

    /**
     * 取消预约
     * @param id
     */
    Boolean cancel(Integer id);

    /**
     * 获取可预约的时间段
     * @return
     */
    List<Map<String, Object>> getWorkDayTimeList();
}
