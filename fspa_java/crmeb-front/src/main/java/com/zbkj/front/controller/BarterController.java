package com.zbkj.front.controller;

import com.zbkj.common.constants.BarterConstants;
import com.zbkj.common.model.barter.EbBarterApply;
import com.zbkj.common.model.barter.EbBarterPointsLog;
import com.zbkj.common.model.barter.EbBarterCreditLog;
import com.zbkj.common.model.barter.EbBarterCategory;
import com.zbkj.common.model.barter.EbBarterItem;
import com.zbkj.common.model.barter.EbBarterMemberLevel;
import com.zbkj.common.model.barter.EbBarterMessage;
import com.zbkj.common.model.barter.EbBarterOrder;
import com.zbkj.common.model.barter.EbBarterOrderMessage;
import com.zbkj.common.model.barter.EbBarterUserAddress;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.BarterChainCreateRequest;
import com.zbkj.common.request.BarterChainSearchRequest;
import com.zbkj.common.request.BarterItemSearchRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.barter.BarterApplyDetailResponse;
import com.zbkj.common.response.barter.BarterChainCreateResponse;
import com.zbkj.common.response.barter.BarterChainSearchResponse;
import com.zbkj.common.response.barter.BarterItemResponse;
import com.zbkj.common.response.barter.BarterOrderDetailResponse;
import com.zbkj.service.service.BarterPointsLogService;
import com.zbkj.common.response.barter.BarterUnreadResponse;
import com.zbkj.common.response.CommonResult;
import com.zbkj.service.service.BarterCreditLogService;
import com.zbkj.service.service.BarterApplyService;
import com.zbkj.service.service.BarterCategoryService;
import com.zbkj.service.service.BarterChainService;
import com.zbkj.service.service.BarterConfigService;
import com.zbkj.service.service.BarterItemLikeService;
import com.zbkj.service.service.BarterItemService;
import com.zbkj.service.service.BarterMemberLevelService;
import com.zbkj.service.service.BarterMessageService;
import com.zbkj.service.service.BarterOrderMessageService;
import com.zbkj.service.service.BarterOrderService;
import com.zbkj.service.service.BarterUserAddressService;
import com.zbkj.service.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 换物模块 前端控制器（小程序/H5 调用，前缀 /api/front/barter）
 */
@RestController
@RequestMapping("api/front/barter")
@Api(tags = "换物模块")
public class BarterController {

    @Autowired
    private BarterItemService barterItemService;
    @Autowired
    private BarterApplyService barterApplyService;
    @Autowired
    private BarterOrderService barterOrderService;
    @Autowired
    private BarterOrderMessageService barterOrderMessageService;
    @Autowired
    private BarterMemberLevelService memberLevelService;
    @Autowired
    private BarterItemLikeService barterItemLikeService;
    @Autowired
    private BarterUserAddressService barterUserAddressService;
    @Autowired
    private BarterMessageService barterMessageService;
    @Autowired
    private BarterCategoryService barterCategoryService;
    @Autowired
    private BarterChainService barterChainService;
    @Autowired
    private BarterConfigService barterConfigService;
    @Autowired
    private UserService userService;
    @Autowired
    private BarterPointsLogService pointsLogService;
    @Autowired
    private BarterCreditLogService creditLogService;

    /* ---------------- 明细（我的页三个统计点进去看） ---------------- */
    @ApiOperation(value = "积分明细")
    @RequestMapping(value = "/points/log", method = RequestMethod.GET)
    public CommonResult<CommonPage<EbBarterPointsLog>> pointsLog(@ModelAttribute PageParamRequest pageRequest) {
        Integer uid = userService.getUserIdException();
        return CommonResult.success(pointsLogService.myLog(uid, pageRequest));
    }

    @ApiOperation(value = "信用分明细")
    @RequestMapping(value = "/credit/log", method = RequestMethod.GET)
    public CommonResult<CommonPage<EbBarterCreditLog>> creditLog(@ModelAttribute PageParamRequest pageRequest) {
        Integer uid = userService.getUserIdException();
        return CommonResult.success(creditLogService.myLog(uid, pageRequest));
    }

    /* ---------------- 物品 ---------------- */
    @ApiOperation(value = "换物品类列表（发布/筛选共用）")
    @RequestMapping(value = "/category/list", method = RequestMethod.GET)
    public CommonResult<List<EbBarterCategory>> categoryList() {
        return CommonResult.success(barterCategoryService.frontList());
    }

    @ApiOperation(value = "好物信息流")
    @RequestMapping(value = "/item/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<BarterItemResponse>> itemList(@ModelAttribute BarterItemSearchRequest request, @ModelAttribute PageParamRequest pageRequest) {
        return CommonResult.success(barterItemService.frontList(request, pageRequest));
    }

    @ApiOperation(value = "物品详情")
    @RequestMapping(value = "/item/info", method = RequestMethod.GET)
    public CommonResult<BarterItemResponse> itemInfo(@RequestParam Integer id) {
        return CommonResult.success(barterItemService.detail(id));
    }

    @ApiOperation(value = "发布好物")
    @RequestMapping(value = "/item/publish", method = RequestMethod.POST)
    public CommonResult<String> publish(@RequestBody EbBarterItem item) {
        return barterItemService.publish(item) ? CommonResult.success() : CommonResult.failed();
    }

    @ApiOperation(value = "编辑好物（物品主人，保存后重新进入审核）")
    @RequestMapping(value = "/item/edit", method = RequestMethod.POST)
    public CommonResult<String> edit(@RequestBody EbBarterItem item) {
        return barterItemService.edit(item) ? CommonResult.success() : CommonResult.failed("物品不存在或无权操作");
    }

    @ApiOperation(value = "重新发起审核（已驳回的好物，不改内容直接重新提交）")
    @RequestMapping(value = "/item/reapply", method = RequestMethod.POST)
    public CommonResult<String> reApply(@RequestParam Integer id) {
        return barterItemService.reApply(id) ? CommonResult.success() : CommonResult.failed("物品不存在或无权操作");
    }

    @ApiOperation(value = "下架好物（仅在售态可下架；下架后不可再被换，需重新发布）")
    @RequestMapping(value = "/item/offshelf", method = RequestMethod.POST)
    public CommonResult<String> offshelf(@RequestParam Integer id) {
        return barterItemService.offshelf(id) ? CommonResult.success() : CommonResult.failed("物品不存在、非在售或无权操作");
    }

    @ApiOperation(value = "继续换下一件（把换物收到的已交换物品重新上架）")
    @RequestMapping(value = "/item/relist", method = RequestMethod.POST)
    public CommonResult<String> relist(@RequestParam Integer id) {
        return barterItemService.relist(id) ? CommonResult.success() : CommonResult.failed("仅能重新上架通过换物收到的物品");
    }

    @ApiOperation(value = "我的发布（status 为空查全部，否则按物品状态过滤）")
    @RequestMapping(value = "/item/mylist", method = RequestMethod.GET)
    public CommonResult<CommonPage<BarterItemResponse>> myList(@ModelAttribute PageParamRequest pageRequest,
                                                              @RequestParam(required = false) Integer status) {
        return CommonResult.success(barterItemService.myList(pageRequest, status));
    }

    @ApiOperation(value = "置顶物品（消耗积分，时长/单价后台可配）")
    @RequestMapping(value = "/item/top", method = RequestMethod.POST)
    public CommonResult<String> top(@RequestParam Integer id,
                                    @RequestParam(required = false) Integer hours) {
        // 不传时长就用后台配置的默认时长（默认 24 小时），避免前端硬编码 1 小时
        return barterItemService.topItem(id, hours) ? CommonResult.success() : CommonResult.failed("无权操作");
    }

    @ApiOperation(value = "置顶计价（单价/默认时长/本次花费/我的积分），供前端展示与二次确认")
    @RequestMapping(value = "/item/top/config", method = RequestMethod.GET)
    public CommonResult<Map<String, Object>> topConfig() {
        int perHour = barterItemService.topPointsPerHour();
        int hours = barterItemService.topDurationHour();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pointsPerHour", perHour);
        m.put("hours", hours);
        m.put("cost", perHour * hours);
        Integer uid = userService.getUserIdException();
        User me = uid == null ? null : userService.getById(uid);
        m.put("myPoints", me == null || me.getBarterPoints() == null ? 0 : me.getBarterPoints());
        return CommonResult.success(m);
    }

    @ApiOperation(value = "切换「我想要」（点赞/取消），返回 true=已想要 false=已取消")
    @RequestMapping(value = "/item/want", method = RequestMethod.POST)
    public CommonResult<Boolean> want(@RequestParam Integer id) {
        Integer uid = userService.getUserIdException();
        return CommonResult.success(barterItemLikeService.toggle(id, uid));
    }

    @ApiOperation(value = "我的想要列表（含已下架/已交换状态标识）")
    @RequestMapping(value = "/item/wantlist", method = RequestMethod.GET)
    public CommonResult<CommonPage<BarterItemResponse>> wantList(@ModelAttribute PageParamRequest pageRequest) {
        return CommonResult.success(barterItemLikeService.myWantList(pageRequest));
    }

    @ApiOperation(value = "有货城市列表（好物页城市筛选用）")
    @RequestMapping(value = "/item/cities", method = RequestMethod.GET)
    public CommonResult<List<String>> cities() {
        return CommonResult.success(barterItemService.onSaleCities());
    }

    /* ---------------- 申请 ---------------- */
    @ApiOperation(value = "发起换物申请")
    @RequestMapping(value = "/apply/create", method = RequestMethod.POST)
    public CommonResult<Integer> applyCreate(@RequestBody EbBarterApply apply) {
        Integer id = barterApplyService.createApply(apply);
        return id > 0 ? CommonResult.success(id) : CommonResult.failed();
    }

    @ApiOperation(value = "接受申请（物品主人；选交付方式：1自提填selfTakeAddress 2快递用双方默认地址）")
    @RequestMapping(value = "/apply/accept", method = RequestMethod.POST)
    public CommonResult<String> applyAccept(@RequestParam Integer applyId,
                                            @RequestParam(required = false) Integer deliveryType,
                                            @RequestParam(required = false) String selfTakeAddress) {
        return barterApplyService.accept(applyId, deliveryType, selfTakeAddress) ? CommonResult.success() : CommonResult.failed();
    }

    @ApiOperation(value = "拒绝申请（物品主人）")
    @RequestMapping(value = "/apply/reject", method = RequestMethod.POST)
    public CommonResult<String> applyReject(@RequestParam Integer applyId) {
        return barterApplyService.reject(applyId) ? CommonResult.success() : CommonResult.failed();
    }

    @ApiOperation(value = "取消申请（申请人）")
    @RequestMapping(value = "/apply/cancel", method = RequestMethod.POST)
    public CommonResult<String> applyCancel(@RequestParam Integer applyId) {
        return barterApplyService.cancel(applyId) ? CommonResult.success() : CommonResult.failed();
    }

    @ApiOperation(value = "收到的申请（status 为空查全部，否则按申请状态过滤）")
    @RequestMapping(value = "/apply/incoming", method = RequestMethod.GET)
    public CommonResult<CommonPage<EbBarterApply>> incoming(@ModelAttribute PageParamRequest pageRequest,
                                                          @RequestParam(required = false) Integer status) {
        return CommonResult.success(barterApplyService.incomingList(pageRequest, status));
    }

    @ApiOperation(value = "我的申请（status 为空查全部，否则按申请状态过滤）")
    @RequestMapping(value = "/apply/mylist", method = RequestMethod.GET)
    public CommonResult<CommonPage<EbBarterApply>> myApply(@ModelAttribute PageParamRequest pageRequest,
                                                          @RequestParam(required = false) Integer status) {
        return CommonResult.success(barterApplyService.myApplyList(pageRequest, status));
    }

    @ApiOperation(value = "申请详情（消息直达二级页）")
    @RequestMapping(value = "/apply/detail", method = RequestMethod.GET)
    public CommonResult<BarterApplyDetailResponse> applyDetail(@RequestParam Integer applyId) {
        BarterApplyDetailResponse vo = barterApplyService.detail(applyId);
        return vo == null ? CommonResult.failed() : CommonResult.success(vo);
    }

    /* ---------------- 换物单 ---------------- */
    @ApiOperation(value = "我的换物单（status 为空查全部；role: 空=全部 0=我发起 1=我收到）")
    @RequestMapping(value = "/order/mylist", method = RequestMethod.GET)
    public CommonResult<CommonPage<BarterOrderDetailResponse>> orderMyList(@ModelAttribute PageParamRequest pageRequest,
                                                              @RequestParam(required = false) Integer status,
                                                              @RequestParam(required = false) Integer role) {
        return CommonResult.success(barterOrderService.myList(pageRequest, status, role));
    }

    @ApiOperation(value = "换物单详情（消息直达二级页，以「我」的视角展开）")
    @RequestMapping(value = "/order/detail", method = RequestMethod.GET)
    public CommonResult<BarterOrderDetailResponse> orderDetail(@RequestParam Integer orderId) {
        BarterOrderDetailResponse vo = barterOrderService.detail(orderId);
        return vo == null ? CommonResult.failed() : CommonResult.success(vo);
    }

    @ApiOperation(value = "根据物品 id 反查它所在的换物单 id（已交换商品→查看明细用），返回 null 表示无关联单")
    @RequestMapping(value = "/order/by-item", method = RequestMethod.GET)
    public CommonResult<Integer> orderByItem(@RequestParam Integer itemId) {
        return CommonResult.success(barterOrderService.getOrderIdByItem(itemId));
    }

    /* ---------------- 换物单：新流程（申请即建单） ---------------- */
    @ApiOperation(value = "发起换物（用我的一件在售物换对方一件在售物，直接生成待确认换物单，返回 orderId）")
    @RequestMapping(value = "/order/create", method = RequestMethod.POST)
    public CommonResult<Integer> orderCreate(@RequestBody Map<String, Object> body) {
        Integer targetItemId = intVal(body.get("targetItemId"));
        Integer offerItemId = intVal(body.get("offerItemId"));
        Integer addressId = intVal(body.get("addressId"));
        Object msg = body.get("applyMessage");
        Integer id = barterOrderService.createOrder(targetItemId, offerItemId, msg == null ? null : String.valueOf(msg), addressId);
        if (id == null) {
            return CommonResult.failed("参数不完整");
        }
        if (id == -2) {
            return CommonResult.failed("对方尚未设置默认收货地址，无法发起快递换物，请提醒对方先填写默认地址");
        }
        if (id == -3) {
            return CommonResult.failed("请先为你的账号设置默认收货地址后再发起快递换物");
        }
        if (id <= 0) {
            return CommonResult.failed("物品不可换（需双方均为在售状态且对方物品已设置交付方式），或快递时未选收货地址");
        }
        return CommonResult.success(id);
    }

    @ApiOperation(value = "接受换物（物品主人；通过后进行中：快递=待发货 自提=待完成）")
    @RequestMapping(value = "/order/accept", method = RequestMethod.POST)
    public CommonResult<String> orderAccept(@RequestParam Integer orderId) {
        return barterOrderService.acceptOrder(orderId) ? CommonResult.success() : CommonResult.failed("状态不符或你不是物主，或物主无默认收货地址");
    }

    @ApiOperation(value = "拒绝换物（物品主人）")
    @RequestMapping(value = "/order/reject", method = RequestMethod.POST)
    public CommonResult<String> orderReject(@RequestParam Integer orderId) {
        return barterOrderService.rejectOrder(orderId) ? CommonResult.success() : CommonResult.failed("状态不符或你不是物主");
    }

    @ApiOperation(value = "取消换物单（双方，仅待确认/进行中）")
    @RequestMapping(value = "/order/cancel", method = RequestMethod.POST)
    public CommonResult<String> orderCancel(@RequestParam Integer orderId) {
        return barterOrderService.frontCancel(orderId) ? CommonResult.success() : CommonResult.failed("当前状态不可取消");
    }

    @ApiOperation(value = "发起纠纷（双方，进行中/待收货）")
    @RequestMapping(value = "/order/dispute", method = RequestMethod.POST)
    public CommonResult<String> orderDispute(@RequestParam Integer orderId) {
        return barterOrderService.dispute(orderId) ? CommonResult.success() : CommonResult.failed("当前状态不可发起纠纷");
    }

    @ApiOperation(value = "换物单内聊天记录（仅双方可见）")
    @RequestMapping(value = "/order/messages", method = RequestMethod.GET)
    public CommonResult<List<EbBarterOrderMessage>> orderMessages(@RequestParam Integer orderId) {
        return CommonResult.success(barterOrderService.orderMessages(orderId));
    }

    @ApiOperation(value = "换物单内发消息（type: 1文本 2图片 3定位）")
    @RequestMapping(value = "/order/message/send", method = RequestMethod.POST)
    public CommonResult<EbBarterOrderMessage> orderMessageSend(@RequestBody EbBarterOrderMessage message) {
        Integer uid = userService.getUserIdException();
        // 校验参与方并自动补全接收方（前端可不传 toUid）
        BarterOrderDetailResponse vo = barterOrderService.detail(message.getOrderId());
        if (vo == null) return CommonResult.failed("换物单不存在或你不是参与方");
        Integer toUid = message.getToUid();
        if (toUid == null) {
            toUid = "owner".equals(vo.getMyRole()) ? (vo.getApplyUser() == null ? null : vo.getApplyUser().getUid())
                    : (vo.getOwnerUser() == null ? null : vo.getOwnerUser().getUid());
        }
        if (toUid == null) return CommonResult.failed("接收方不存在");
        EbBarterOrderMessage saved = barterOrderMessageService.send(message.getOrderId(), uid, toUid,
                message.getType(), message.getContent(), message.getLat(), message.getLng(), message.getAddress());
        return saved == null ? CommonResult.failed("发送失败") : CommonResult.success(saved);
    }

    private Integer intVal(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.valueOf(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }

    @ApiOperation(value = "标记交付（role: owner/apply；快递传 logisticsCompany + logisticsNo）")
    @RequestMapping(value = "/order/deliver", method = RequestMethod.POST)
    public CommonResult<String> deliver(@RequestParam Integer orderId, @RequestParam String role,
                                        @RequestParam(required = false) String logisticsCompany,
                                        @RequestParam(required = false) String logisticsNo) {
        return barterOrderService.deliver(orderId, role, logisticsCompany, logisticsNo) ? CommonResult.success() : CommonResult.failed();
    }

    @ApiOperation(value = "改快递单号（发货后 24 小时内、最多 3 次，只能改自己这一侧）")
    @RequestMapping(value = "/order/express/update", method = RequestMethod.POST)
    public CommonResult<String> updateExpress(@RequestParam Integer orderId, @RequestParam String role,
                                              @RequestParam String logisticsCompany,
                                              @RequestParam String logisticsNo) {
        return barterOrderService.updateExpress(orderId, role, logisticsCompany, logisticsNo)
                ? CommonResult.success() : CommonResult.failed("修改失败");
    }

    /* ---------------- 智能换物（路径规划 + 只走第一棒） ---------------- */

    @ApiOperation(value = "智能换物-档位（2 人 / 3 人 / 4-6 人，金额与上限后台可配）")
    @RequestMapping(value = "/chain/points", method = RequestMethod.GET)
    public CommonResult<List<Map<String, Object>>> chainPoints() {
        // 档位金额与最大跳数均可在后台「规则配置」调整，这里只负责组织展示结构
        // 第三档 hops 取上限（默认 5 = 最多 6 人），检索时按 4/5/6 人依次找；
        // 实际收费由成交方案的人数决定，这一档无论 4 人还是上限人数都落在同一档价。
        int maxHops = barterConfigService.chainMaxHops();
        int[][] tiers = {{1, 2}, {2, 3}, {maxHops, maxHops + 1}};
        List<Map<String, Object>> list = new ArrayList<>();
        for (int[] tier : tiers) {
            int hops = tier[0];
            int people = tier[1];
            int fee = barterConfigService.pathFee(people - 1);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("hops", hops);
            m.put("people", people);
            m.put("peopleLabel", people == 2 ? "2 人" : (people == 3 ? "3 人" : "4-" + (maxHops + 1) + " 人"));
            m.put("points", fee);
            m.put("free", fee == 0);
            // 收费统一在「第一棒成交」这一刻；后续几棒平台不担保，也就不再额外收费
            m.put("feeOnDeal", true);
            // 档位卡只有三列宽度，说明必须短到一行放得下
            m.put("desc", hops == 1 ? "直接互换"
                    : (people == 3 ? "1 位中间人" : "2-" + (maxHops - 1) + " 位中间人"));
            list.add(m);
        }
        return CommonResult.success(list);
    }

    @ApiOperation(value = "智能换物-检索交换环（免费；不扣积分，可反复试）")
    @RequestMapping(value = "/chain/search", method = RequestMethod.GET)
    public CommonResult<BarterChainSearchResponse> chainSearch(@ModelAttribute BarterChainSearchRequest request) {
        return CommonResult.success(barterChainService.search(request));
    }

    @ApiOperation(value = "智能换物-发起第一棒（只生成 1 张 1v1 单，不扣积分；成交时才收路径费）")
    @RequestMapping(value = "/chain/create", method = RequestMethod.POST)
    public CommonResult<BarterChainCreateResponse> chainCreate(@RequestBody BarterChainCreateRequest request) {
        return CommonResult.success(barterChainService.createChain(request));
    }

    @ApiOperation(value = "智能换物-环详情（环上参与者可见，展开每张 1v1 单的状态）")
    @RequestMapping(value = "/chain/detail", method = RequestMethod.GET)
    public CommonResult<BarterChainCreateResponse.Detail> chainDetail(@RequestParam String chainNo) {
        return CommonResult.success(barterChainService.detail(chainNo));
    }

    /* ---------------- 快递公司 ---------------- */
    @ApiOperation(value = "换物单物流轨迹（role: owner/apply，缺省取「我」这一侧）")
    @RequestMapping(value = "/order/express", method = RequestMethod.GET)
    public CommonResult<Map<String, Object>> orderExpress(@RequestParam Integer orderId,
                                                          @RequestParam(required = false) String role) {
        return CommonResult.success(barterOrderService.expressTrace(orderId, role));
    }

    @ApiOperation(value = "快递公司列表（发货时下拉选择）")
    @RequestMapping(value = "/express/list", method = RequestMethod.GET)
    public CommonResult<List<Map<String, String>>> expressList() {
        return CommonResult.success(BarterConstants.expressList());
    }

    /* ---------------- 站内消息 ---------------- */
    @ApiOperation(value = "我的消息（分页；group 为空查全部：incoming/outgoing/order/want/system）")
    @RequestMapping(value = "/message/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<EbBarterMessage>> messageList(@RequestParam(required = false) String group,
                                                                 @ModelAttribute PageParamRequest pageRequest) {
        return CommonResult.success(barterMessageService.myList(group, pageRequest));
    }

    @ApiOperation(value = "未读汇总（各业务条目红点数字）")
    @RequestMapping(value = "/message/unread", method = RequestMethod.GET)
    public CommonResult<BarterUnreadResponse> messageUnread() {
        return CommonResult.success(barterMessageService.unreadSummary());
    }

    @ApiOperation(value = "标记单条消息已读")
    @RequestMapping(value = "/message/read", method = RequestMethod.POST)
    public CommonResult<Boolean> messageRead(@RequestParam Integer id) {
        return CommonResult.success(barterMessageService.markRead(id));
    }

    @ApiOperation(value = "标记整组已读（group 为空则全部已读）")
    @RequestMapping(value = "/message/readgroup", method = RequestMethod.POST)
    public CommonResult<Boolean> messageReadGroup(@RequestParam(required = false) String group) {
        return CommonResult.success(barterMessageService.markGroupRead(group));
    }

    @ApiOperation(value = "确认收货（role: owner/apply，双方确认后自动完成）")
    @RequestMapping(value = "/order/confirm", method = RequestMethod.POST)
    public CommonResult<String> confirm(@RequestParam Integer orderId, @RequestParam String role) {
        return barterOrderService.confirmRecv(orderId, role) ? CommonResult.success() : CommonResult.failed();
    }

    /* ---------------- 等级 ---------------- */
    @ApiOperation(value = "会员等级列表")
    @RequestMapping(value = "/level/list", method = RequestMethod.GET)
    public CommonResult<List<EbBarterMemberLevel>> levelList() {
        return CommonResult.success(memberLevelService.tree());
    }

    /* ---------------- 收货地址 ---------------- */
    @ApiOperation(value = "我的收货地址")
    @RequestMapping(value = "/address/list", method = RequestMethod.GET)
    public CommonResult<List<EbBarterUserAddress>> addressList() {
        return CommonResult.success(barterUserAddressService.myList(userService.getUserIdException()));
    }

    @ApiOperation(value = "新增收货地址")
    @RequestMapping(value = "/address/add", method = RequestMethod.POST)
    public CommonResult<Boolean> addressAdd(@RequestBody EbBarterUserAddress address) {
        return CommonResult.success(barterUserAddressService.add(userService.getUserIdException(), address));
    }

    @ApiOperation(value = "修改收货地址")
    @RequestMapping(value = "/address/edit", method = RequestMethod.POST)
    public CommonResult<Boolean> addressEdit(@RequestBody EbBarterUserAddress address) {
        return CommonResult.success(barterUserAddressService.edit(userService.getUserIdException(), address));
    }

    @ApiOperation(value = "删除收货地址")
    @RequestMapping(value = "/address/delete", method = RequestMethod.POST)
    public CommonResult<Boolean> addressDelete(@RequestParam Integer id) {
        return CommonResult.success(barterUserAddressService.delete(userService.getUserIdException(), id));
    }

    @ApiOperation(value = "设置默认地址")
    @RequestMapping(value = "/address/default", method = RequestMethod.POST)
    public CommonResult<Boolean> addressDefault(@RequestParam Integer id) {
        return CommonResult.success(barterUserAddressService.setDefault(userService.getUserIdException(), id));
    }

    /* ---------------- 个人城市 ---------------- */
    @ApiOperation(value = "设置我的换物城市")
    @RequestMapping(value = "/user/city", method = RequestMethod.POST)
    public CommonResult<Boolean> setCity(@RequestParam String city) {
        Integer uid = userService.getUserIdException();
        User user = new User();
        user.setUid(uid);
        user.setBarterCity(city);
        return CommonResult.success(userService.updateById(user));
    }
}
