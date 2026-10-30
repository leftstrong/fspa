package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.article.EbArtisanEntity;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.ArtisanDetailResponse;
import com.zbkj.common.response.ArtisanSearchRequest;
import com.zbkj.common.vo.MyRecord;

import java.util.List;

/**
 * 匠人表 业务接口
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
public interface EbArtisanService extends IService<EbArtisanEntity> {

    /**
     * EbArtisan 列表查询
     *
     * @param request          默认是是体类 根据自己需求修改或者创建自己的request
     * @param pageParamRequest 分页参数对象
     * @return
     */
    CommonPage<ArtisanDetailResponse> adminList(ArtisanSearchRequest request, PageParamRequest pageParamRequest);

    /**
     *  根据预约时间段：筛门店下的手艺人
     * @param appointmentTime
     * @return
     */
    List<ArtisanDetailResponse> listByAppointmentTime (String appointmentTime);

    ArtisanDetailResponse getCraftsmanInfoById(Integer craftsmanId);
}
