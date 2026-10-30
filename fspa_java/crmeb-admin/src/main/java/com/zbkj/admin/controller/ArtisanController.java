package com.zbkj.admin.controller;

import com.github.pagehelper.util.StringUtil;
import com.qcloud.cos.utils.Jackson;
import com.qcloud.cos.utils.StringUtils;
import com.zbkj.common.model.article.EbArtisanEntity;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.ArtisanDetailResponse;
import com.zbkj.common.response.ArtisanSearchRequest;
import com.zbkj.common.response.CommonResult;
import com.zbkj.common.response.EbArtisanInfoResponse;
import com.zbkj.common.utils.DateUtil;
import com.zbkj.service.service.EbArtisanService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 匠人表 控制器
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2022 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: 大粽子
 * +----------------------------------------------------------------------
 * ｜ @date Thu May 18 08:44:58 CST 2023
 * +----------------------------------------------------------------------
 * ｜ @date stivepeim@outlook.com
 * +----------------------------------------------------------------------
 */
@RestController
@RequestMapping("api/admin/artisan")
public class ArtisanController {
    @Autowired
    private EbArtisanService ebArtisanService;

    /**
     * 分页显示订单表
     *
     * @param request          搜索条件
     * @param pageParamRequest 分页参数
     */
//    @PreAuthorize("hasAuthority('admin:order:list')")
    @ApiOperation(value = "分页列表") //配合swagger使用
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<ArtisanDetailResponse>> getList(@Validated ArtisanSearchRequest request, @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(ebArtisanService.adminList(request, pageParamRequest));
    }


    /**
     * 详情数据
     */
//    @RequestMapping("/info/{id}')")
//    @PreAuthorize("hasAuthority('admin:artisan:info')")
    @RequestMapping(value = "/info", method = RequestMethod.GET)
    public CommonResult<EbArtisanInfoResponse> info(@RequestParam(value = "id") Integer id) {
        EbArtisanEntity ebArtisan = ebArtisanService.getById(id);
        EbArtisanInfoResponse ebArtisanInfoResponse = new EbArtisanInfoResponse();
        BeanUtils.copyProperties(ebArtisan, ebArtisanInfoResponse);
        List<String> timeList = new ArrayList<>(Arrays.asList("10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00"));
        List<String> numberDays = DateUtil.getNumberDays(7);
        Map<String, Integer> dateIdMap = new HashMap<>();
        int dateId = 1;
        for (String numberDay : numberDays) {
            dateIdMap.put(numberDay, dateId);
            dateId++;
        }
        Map<String, Integer> dateTimeIdMap = new HashMap<>();
        int dateTimeId = 8;
        for (String numberDay : numberDays) {
            for (String time : timeList) {
                dateTimeIdMap.put(numberDay + time, dateTimeId);
                dateTimeId++;
            }
        }
        List<Map<String, Object>> resultList = new ArrayList<>();
        List<Integer> expandedKeys = new ArrayList<>();
        List<Integer> checkedKeys = new ArrayList<>();
        if (!StringUtil.isEmpty(ebArtisan.getWorkTime())) {
            StringBuffer workTimeStr = new StringBuffer();
            Map<String, List<String>> workTime = Jackson.fromJsonString(ebArtisan.getWorkTime(), Map.class);
            Map<String, Object> workDayTimeMap;
            for (String s : workTime.keySet()) {
                workDayTimeMap = new HashMap<>();
                List<String> strings = workTime.get(s);
                Map<String, Object> timeMap;
                List<Map<String, Object>> timeMapList = new ArrayList<>();
                for (String string : strings) {
                    timeMap = new HashMap<>();
                    workTimeStr.append(s + " " + string).append(",");
                    timeMap.put("label", string);
                    Integer expandedKey = dateTimeIdMap.get(s + string);
                    checkedKeys.add(expandedKey);
                    timeMap.put("id", expandedKey);
                    timeMap.put("date", s);
                    timeMapList.add(timeMap);
                }
                workDayTimeMap.put("label", s);
                Integer expandedKey = dateIdMap.get(s);
                expandedKeys.add(expandedKey);
                workDayTimeMap.put("id", expandedKey);
                workDayTimeMap.put("timeList", timeMapList);
                resultList.add(workDayTimeMap);
            }
            ebArtisanInfoResponse.setWorkTime(workTimeStr.toString());
            ebArtisanInfoResponse.setWorkTimeMap(workTime);
            ebArtisanInfoResponse.setWorkTimeMapList(resultList);
            ebArtisanInfoResponse.setExpandedKeys(expandedKeys.stream().filter(Objects::nonNull).collect(Collectors.toList()));
            ebArtisanInfoResponse.setCheckedKeys(checkedKeys.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        }
        List<Map<String, Object>> emptyWorkTimeMapList = new ArrayList<>();
        Map<String, Object> numberDayTimeMap;
        for (String numberDay : numberDays) {
            numberDayTimeMap = new HashMap<>();
            numberDayTimeMap.put("label", numberDay);
            numberDayTimeMap.put("id", dateIdMap.get(numberDay));
            Map<String, Object> timeMap;
            List<Map<String, Object>> timeMapList = new ArrayList<>();
            for (String time : timeList) {
                timeMap = new HashMap<>();
                timeMap.put("dateTime", numberDay + " " + time);
                timeMap.put("label", time);
                timeMap.put("id", dateTimeIdMap.get(numberDay + time));
                timeMapList.add(timeMap);
            }
            numberDayTimeMap.put("timeList", timeMapList);
            emptyWorkTimeMapList.add(numberDayTimeMap);
        }

        ebArtisanInfoResponse.setEmptyWorkTimeMapList(emptyWorkTimeMapList);
        return CommonResult.success(ebArtisanInfoResponse);
    }

    /**
     * 新增数据
     */
    @ApiOperation(value = "新增")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public CommonResult<String> save(@RequestBody EbArtisanEntity ebArtisan) {
//        ebArtisan.setAvatar("crmebimage/public/operation/2023/06/07/94ab3bf638a64400b30e9a839bb8e3aaenenhzhhlv.jpg");
        ebArtisan.setAvatar(ebArtisan.getAvatar().replace("https://api.image.flowerspa.cn/", ""));
        //2023-06-16 9:00,2023-06-16 11:00
        if (!StringUtils.isNullOrEmpty(ebArtisan.getWorkTime())) {
            String[] splitTime = ebArtisan.getWorkTime().split(",");
            Map<String, List<String>> dateMap = new HashMap<>();
            for (String time : splitTime) {
                String[] temp = time.split(" ");
                if (dateMap.containsKey(temp[0])) {
                    List<String> strings = dateMap.get(temp[0]);
                    strings.add(temp[1]);
                    continue;
                }
                List<String> timeList = new ArrayList<>();
                timeList.add(temp[1]);
                dateMap.put(temp[0], timeList);
            }
            ebArtisan.setWorkTime(Jackson.toJsonString(dateMap));
        }
        if (ebArtisanService.saveOrUpdate(ebArtisan)) {
            return CommonResult.success();
        }
        return CommonResult.failed();
    }

//    /**
//     * 修改数据
//     */
////    @RequestMapping("/update')")
//    @ApiOperation(value = "修改")
//    @RequestMapping(value = "/update", method = RequestMethod.POST)
////    @PreAuthorize("hasAuthority('admin:artisan:update')")
//    public CommonResult<String> update(@RequestBody EbArtisanEntity ebArtisan){
//        if (ebArtisanService.updateById(ebArtisan)) {
//            return CommonResult.success();
//        }
//        return CommonResult.failed();
//    }

    /**
     * 删除:根据id集合
     */
//    @RequestMapping("/delete')")
//    @PreAuthorize("hasAuthority('admin:artisan:delete')")
    @RequestMapping(value = "/delete", method = RequestMethod.GET)
    public CommonResult<String> delete(@RequestParam(value = "id") Integer id) {
        if (ebArtisanService.removeById(id)) {
            return CommonResult.success();
        }
        return CommonResult.failed();
    }

}
