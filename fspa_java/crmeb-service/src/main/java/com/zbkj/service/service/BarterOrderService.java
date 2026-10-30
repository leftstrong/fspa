package com.zbkj.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.barter.EbBarterOrder;
import com.zbkj.common.model.barter.EbBarterOrderMessage;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.BarterOrderSearchRequest;
import com.zbkj.common.response.barter.BarterOrderDetailResponse;

import java.util.List;

/**
 * 换物单 业务接口
 * 状态枚举：0待确认 1已取消 2进行中(快递=待发货/自提=待完成) 3待收货 4已完成 5已拒绝 6纠纷中
 */
public interface BarterOrderService extends IService<EbBarterOrder> {

    /** 我参与的换物单（富化视图），status 为空查全部；role: null=全部 0=我发起 1=我收到 */
    CommonPage<BarterOrderDetailResponse> myList(PageParamRequest pageParamRequest, Integer status, Integer role);

    /** 管理后台分页 */
    CommonPage<EbBarterOrder> adminList(BarterOrderSearchRequest request, PageParamRequest pageParamRequest);

    /**
     * 后台换物单列表（富化版）：在原始字段基础上补齐物品标题、双方昵称，
     * 避免运营对着 uid 猜人。返回 Map 是为了不改动既有的 adminList 契约。
     */
    CommonPage<java.util.Map<String, Object>> adminRichList(BarterOrderSearchRequest request, PageParamRequest pageParamRequest);

    /** 一方标记交付（role: owner/apply；快递时传 logisticsCompany + logisticsNo） */
    Boolean deliver(Integer orderId, String role, String logisticsCompany, String logisticsNo);

    /** 发货后 24 小时内修改自己这一侧的快递单号（最多 3 次，超时转客服） */
    Boolean updateExpress(Integer orderId, String role, String logisticsCompany, String logisticsNo);

    /** 一方确认收货 */
    Boolean confirmRecv(Integer orderId, String role);

    /** 双方确认收货后完成：双方成功次数+1、重算等级、信用+5（核心事务） */
    Boolean complete(Integer orderId);

    /** 系统自动完成：双方确认收货并结算（定时任务调用） */
    Boolean autoCompleteBySystem(Integer orderId);

    /** 取消 / 进入纠纷 */
    Boolean cancel(Integer orderId, String reason);

    /** 换物单详情（消息直达二级页用）：以「我」的视角展开物流与操作权限，非双方返回 null */
    BarterOrderDetailResponse detail(Integer orderId);

    /** 后台换物单详情：双方信息 + 双方物流 + 聊天记录 + 互评，不做视角折叠 */
    com.zbkj.common.response.barter.BarterOrderAdminDetailResponse adminDetail(Integer orderId);

    /* ---------------- 新流程：申请页直接生成换物单 ---------------- */

    /** 发起换物：直接用「我的一件在售物」换「对方一件在售物」，生成待确认换物单，返回 orderId（-1 表示校验失败） */
    Integer createOrder(Integer targetItemId, Integer offerItemId, String applyMessage, Integer addressId);

    /** 物主接受：进入进行中（快递=待发货 / 自提=待完成），自动带出物主默认收货地址 */
    Boolean acceptOrder(Integer orderId);

    /** 物主拒绝：已拒绝 */
    Boolean rejectOrder(Integer orderId);

    /** 前端取消：仅待确认/进行中可取消 */
    Boolean frontCancel(Integer orderId);

    /** 任一方发起纠纷：进行中/待收货可发起，进入纠纷中 */
    Boolean dispute(Integer orderId);

    /** 换物单内聊天记录，仅双方可见 */
    List<EbBarterOrderMessage> orderMessages(Integer orderId);

    /** 根据物品 id 反查它所在的换物单 id（已交换商品→查看明细用）；优先已完成单 */
    Integer getOrderIdByItem(Integer itemId);

    /**
     * 查询换物单某一方的快递轨迹
     * @param orderId 换物单 id
     * @param role owner=物主 / apply=申请人；为空时取「我」自己那一侧
     * @return available=false 时 tip 说明原因（非快递单 / 未填单号 / 查询服务未配置等）
     */
    java.util.Map<String, Object> expressTrace(Integer orderId, String role);
}
