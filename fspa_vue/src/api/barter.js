import request from '@/utils/request'

/* ============ 以物换物 物品审核 ============ */
export function barterItemListApi(params) {
  return request({ url: 'admin/barter/item/list', method: 'GET', params })
}
export function barterItemInfoApi(params) {
  return request({ url: 'admin/barter/item/info', method: 'GET', params })
}
// 注意：后端 audit/pass、audit/reject、down 均为 @RequestParam，需用 params 传参
export function barterItemAuditPassApi(id) {
  return request({ url: 'admin/barter/item/audit/pass', method: 'POST', params: { id } })
}
export function barterItemAuditRejectApi(id, reason) {
  return request({ url: 'admin/barter/item/audit/reject', method: 'POST', params: { id, reason } })
}
export function barterItemDownApi(id) {
  return request({ url: 'admin/barter/item/down', method: 'POST', params: { id } })
}

/* ============ 换物单 ============ */
export function barterOrderListApi(params) {
  return request({ url: 'admin/barter/order/list', method: 'GET', params })
}
export function barterOrderInfoApi(params) {
  return request({ url: 'admin/barter/order/info', method: 'GET', params })
}
// 后端 cancel 为 @RequestParam(id, reason)
export function barterOrderCancelApi(id, reason) {
  return request({ url: 'admin/barter/order/cancel', method: 'POST', params: { id, reason } })
}

/* ============ 会员等级配置 ============ */
export function barterLevelListApi() {
  return request({ url: 'admin/barter/level/list', method: 'GET' })
}
// 后端 save/update 为 @RequestBody
export function barterLevelSaveApi(data) {
  return request({ url: 'admin/barter/level/save', method: 'POST', data })
}
export function barterLevelUpdateApi(data) {
  return request({ url: 'admin/barter/level/update', method: 'POST', data })
}
// 后端 delete 为 @RequestParam(id)；默认等级(id=1)删除会被后端拒绝
export function barterLevelDeleteApi(id) {
  return request({ url: 'admin/barter/level/delete', method: 'POST', params: { id } })
}

/* ============ 品类管理 ============ */
export function barterCategoryListApi() {
  return request({ url: 'admin/barter/category/list', method: 'GET' })
}
// 后端 save/update 为 @RequestBody
export function barterCategorySaveApi(data) {
  return request({ url: 'admin/barter/category/save', method: 'POST', data })
}
export function barterCategoryUpdateApi(data) {
  return request({ url: 'admin/barter/category/update', method: 'POST', data })
}
// 后端 delete 为 @RequestParam
export function barterCategoryDeleteApi(params) {
  return request({ url: 'admin/barter/category/delete', method: 'GET', params })
}

/* ============ 举报处理 ============ */
export function barterReportListApi(params) {
  return request({ url: 'admin/barter/report/list', method: 'GET', params })
}
// 富化列表：含物品图与标题、举报人与被举报人昵称头像
export function barterReportRichListApi(params) {
  return request({ url: 'admin/barter/report/listRich', method: 'GET', params })
}
// 各状态数量，供 TAB 红点
export function barterReportCountsApi() {
  return request({ url: 'admin/barter/report/counts', method: 'GET' })
}
// 举报明细：物品、关联换物单、双方档案、同一被举报人的其他待处理举报
export function barterReportDetailApi(id) {
  return request({ url: 'admin/barter/report/detail', method: 'GET', params: { id } })
}
// 后端 handle 为 @RequestParam(id, status, result, handleUid)；handleUid 不传则取登录态
export function barterReportHandleApi(data) {
  return request({ url: 'admin/barter/report/handle', method: 'POST', params: data })
}

/* ============ 换物配置（复用系统配置接口 SystemConfig）============ */
// 说明：置顶积分/超时等走 eb_system_config，后端以 getValueByKey 读取。
// getuniq 为 GET @RequestParam(key)，返回 res.data 即配置值（字符串）
export function barterConfigGetApi(key) {
  return request({ url: 'admin/system/config/getuniq', method: 'GET', params: { key } })
}
// saveuniq 为 POST @RequestParam(key, value)，必须用 params 传参
export function barterConfigSaveApi(key, value) {
  return request({ url: 'admin/system/config/saveuniq', method: 'POST', params: { key, value } })
}

/* ============ 用户信用/积分看板（后台 oversight）============ */
// keyword 传 uid 或 昵称，返回用户换物汇总（含等级名）
export function barterUserStatApi(keyword) {
  return request({ url: 'admin/barter/user/stat', method: 'GET', params: { keyword } })
}
// 积分明细（按 uid 分页）
export function barterUserPointsLogApi(params) {
  return request({ url: 'admin/barter/user/points/log', method: 'GET', params })
}
// 信用分明细（按 uid 分页）
export function barterUserCreditLogApi(params) {
  return request({ url: 'admin/barter/user/credit/log', method: 'GET', params })
}

/* ============ 换物用户管理 ============ */
// 用户列表（keyword 支持 uid/手机号/昵称；sort 1注册 2积分 3信用 4交换次数）
export function barterUserListApi(params) {
  return request({ url: 'admin/barter/user/list', method: 'GET', params })
}
// 用户详情（档案 + 最近换物单）
export function barterUserDetailApi(uid) {
  return request({ url: 'admin/barter/user/detail', method: 'GET', params: { uid } })
}
// 用户下拉（供换物单按用户筛选）
export function barterUserOptionsApi(keyword) {
  return request({ url: 'admin/barter/user/options', method: 'GET', params: { keyword } })
}
// 启用/禁用用户
export function barterUserStatusApi(uid, status) {
  return request({ url: 'admin/barter/user/status', method: 'POST', params: { uid, status } })
}
// 某用户名下物品（分页，可按状态筛）
export function barterUserItemsApi(params) {
  return request({ url: 'admin/barter/user/items', method: 'GET', params })
}

/* ============ 换物单后台详情 ============ */
// 富化列表：含物品标题与双方昵称，避免运营对着 uid 猜人
export function barterOrderRichListApi(params) {
  return request({ url: 'admin/barter/order/listRich', method: 'GET', params })
}
// 含双方信息、双方物流、聊天记录、互评
export function barterOrderDetailApi(id) {
  return request({ url: 'admin/barter/order/detail', method: 'GET', params: { id } })
}

/* ============ 数据驾驶舱 ============ */
export function barterDashboardSummaryApi() {
  return request({ url: 'admin/barter/dashboard/summary', method: 'GET' })
}
export function barterDashboardTrendApi(days) {
  return request({ url: 'admin/barter/dashboard/trend', method: 'GET', params: { days } })
}
export function barterDashboardDistributeApi() {
  return request({ url: 'admin/barter/dashboard/distribute', method: 'GET' })
}
